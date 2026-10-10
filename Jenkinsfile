pipeline {
  agent none   // every stage picks its own agent

  options {
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds()      // one build at a time; a new push waits in the queue
  }

  environment {
    TAG     = "${BUILD_NUMBER}"                        // image tag for this build
    COMPOSE = 'docker compose -p ecommerce -f compose.yml'
  }

  stages {
    stage('Build & test') {
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
                docker images 'ecommerce/*' --format '{{.Repository}}:{{.Tag}}' \
                  | grep -vE ":($TAG|last-good)$" \
                  | xargs -r docker rmi
                docker image prune -f
              '''
            }
            failure {
              script {
                // Roll back only if a previous healthy release exists
                if (sh(returnStatus: true, script: 'docker image inspect ecommerce/api-gateway:last-good > /dev/null 2>&1') == 0) {
                  echo "Deploy of build ${TAG} is unhealthy: rolling back to last-good"
                  withCredentials([file(credentialsId: 'ecommerce-env', variable: 'APP_ENV')]) {
                    sh 'TAG=last-good $COMPOSE --env-file "$APP_ENV" up -d --wait --wait-timeout 300'
                  }
                  env.ROLLED_BACK = 'true'   // reported in the email
                } else {
                  echo 'Deploy failed and there is no previous good release to roll back to'
                }
              }
            }
          }
        }
      }
    }
  }

  post {
    success {
      emailext(
        to: '$DEFAULT_RECIPIENTS',
        subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} deployed",
        body: """Build #${env.BUILD_NUMBER} passed all tests and is deployed.
          Running images: ecommerce/*:${env.TAG}

          Changes: ${env.BUILD_URL}changes
          Console: ${env.BUILD_URL}console
        """
      )
    }
    failure {
      emailext(
        to: '$DEFAULT_RECIPIENTS',
        subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}" + (env.ROLLED_BACK ? ' (rolled back)' : ''),
        body: """Build #${env.BUILD_NUMBER} failed.
          ${env.ROLLED_BACK ? 'The new release was unhealthy, so the previous good release (last-good) was redeployed. The site is still up.' : 'Nothing new was deployed: the running release is unchanged.'}

          Test results: ${env.BUILD_URL}testReport
          Console: ${env.BUILD_URL}console
        """
      )
    }
  }
}