pipeline {
  agent none   // every stage picks its own agent

  options {
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    TAG     = "${BUILD_NUMBER}"                        // image tag for this build
    COMPOSE = 'docker compose -p ecommerce -f compose.yml'
  }

  stages {
    stage('Test') {
      parallel {
        stage('Backend tests') {
          agent { label 'backend' }
          environment {
            JWT_SECRET            = 'Y2ktdGVzdC1vbmx5LWp3dC1rZXktbm90LWEtcmVhbC1zZWNyZXQ='
            SERVER_SSL_ENABLED    = 'false'
            SSL_KEYSTORE_PASSWORD = 'unused'
          }
          steps {
            sh '''
              for svc in discovery-server api-gateway user-service product-service media-service; do
                echo "=== Testing $svc"
                (cd backend/$svc && MONGODB_URI="mongodb://mongo-test:27017/$svc-test" ./mvnw -B -ntp clean test)
              done
            '''
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: 'backend/*/target/surefire-reports/*.xml'
            }
          }
        }

        stage('Frontend tests') {
          agent { label 'frontend' }
          steps {
            dir('frontend') {
              sh 'npm ci'
              sh 'npx ng test --watch=false'
            }
          }
        }
      }
    }

    // Runs only if every test passed. Both stages share one agent and checkout.
    stage('Deliver') {
      agent { label 'backend' }
      stages {
        stage('Build images') {
          steps {
            withCredentials([file(credentialsId: 'ecommerce-env', variable: 'APP_ENV')]) {
              sh '''
                set +x                           # don't echo commands: they contain the keystore password
                set -a; . "$APP_ENV"; set +a     # load the app's settings (keystore password)

                # The gateway's keystore is git-ignored and baked into its image
                KS=backend/api-gateway/src/main/resources/keystore.p12
                rm -f "$KS"
                keytool -genkeypair -noprompt -alias gateway -keyalg RSA -keysize 2048 \
                  -storetype PKCS12 -validity 3650 -keystore "$KS" -dname "CN=localhost" \
                  -storepass "$SSL_KEYSTORE_PASSWORD" -keypass "$SSL_KEYSTORE_PASSWORD"

                $COMPOSE --env-file "$APP_ENV" build
              '''
            }
          }
        }

        stage('Deploy') {
          steps {
            withCredentials([file(credentialsId: 'ecommerce-env', variable: 'APP_ENV')]) {
              // --wait: succeed only when every container reports healthy
              sh '$COMPOSE --env-file "$APP_ENV" up -d --wait --wait-timeout 300'
            }
          }
          post {
            success {
              // This release is healthy: remember it as the one to roll back to
              sh '''
                for svc in discovery-server api-gateway user-service product-service media-service frontend; do
                  docker tag ecommerce/$svc:$TAG ecommerce/$svc:last-good
                done
              '''
            }
            failure {
              withCredentials([file(credentialsId: 'ecommerce-env', variable: 'APP_ENV')]) {
                sh '''
                  if docker image inspect ecommerce/api-gateway:last-good >/dev/null 2>&1; then
                    echo "Deploy of build $TAG is unhealthy: rolling back to last-good"
                    TAG=last-good $COMPOSE --env-file "$APP_ENV" up -d --wait --wait-timeout 300
                  else
                    echo "Deploy failed and there is no previous good release to roll back to"
                  fi
                '''
              }
            }
          }
        }
      }
    }
  }
}