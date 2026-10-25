pipeline {
  agent none   // every stage picks its own agent

  options {
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds()      // one build at a time per branch/PR; a new push waits in the queue
  }

  triggers {
    // Nightly test + analysis of main, so code health is re-checked even without pushes.
    // In a Multibranch job every branch/PR has its own triggers: only main gets a schedule.
    cron(env.BRANCH_NAME == 'main' ? 'H 3 * * *' : '')
  }

  environment {
    TAG     = "${BUILD_NUMBER}"                        // image tag for this build
    COMPOSE = 'docker compose -p ecommerce -f compose.yml'
    BACKEND_SVCS = 'discovery-server api-gateway user-service product-service media-service order-service'
  }

  stages {
    // Runs before the parallel branches, so both see SONAR_ARGS
    stage('Prepare') {
      steps {
        script {
          // Tell SonarQube what is being analysed: a PR (compared against its target) or a branch
          env.SONAR_ARGS = env.CHANGE_ID
            ? "-Dsonar.pullrequest.key=${env.CHANGE_ID} -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} -Dsonar.pullrequest.base=${env.CHANGE_TARGET}"
            : "-Dsonar.branch.name=${env.BRANCH_NAME}"
        }
      }
    }

    stage('Build & test') {
      parallel {
        // One agent + one workspace for tests and analysis: the analysis reuses
        // the compiled classes and the JaCoCo reports produced by the tests
        stage('Backend') {
          agent { label 'backend' }
          environment {
            JWT_SECRET            = 'Y2ktdGVzdC1vbmx5LWp3dC1rZXktbm90LWEtcmVhbC1zZWNyZXQ='
            SERVER_SSL_ENABLED    = 'false'
            SSL_KEYSTORE_PASSWORD = 'unused'
            CLOUDINARY_URL        = 'cloudinary://test-key:test-secret@test-cloud'
          }
          stages {
            stage('Backend tests') {
              steps {
                sh '''
                  for svc in $BACKEND_SVCS; do
                    echo "=== Testing $svc"
                    (cd backend/$svc && MONGODB_URI="mongodb://mongo-test:27017/$svc-test" ./mvnw -B -ntp clean \
                      org.jacoco:jacoco-maven-plugin:0.8.15:prepare-agent \
                      test \
                      org.jacoco:jacoco-maven-plugin:0.8.15:report)
                  done
                '''
              }
              post {
                always {
                  junit allowEmptyResults: true, testResults: 'backend/*/target/surefire-reports/*.xml'
                }
              }
            }

            stage('Backend analysis') {
              steps {
                fetchTargetBranch()
                withSonarQubeEnv('sonarqube') {
                  // qualitygate.wait: the scanner exits non-zero if the quality gate fails
                  sh '''
                    for svc in $BACKEND_SVCS; do
                      echo "=== Analysing $svc"
                      (cd backend/$svc && ./mvnw -B -ntp \
                        org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                        -Dsonar.projectKey=buy-02-$svc \
                        "-Dsonar.projectName=buy-02 $svc" \
                        -Dsonar.qualitygate.wait=true \
                        -Dsonar.qualitygate.timeout=300 \
                        $SONAR_ARGS)
                    done
                  '''
                }
              }
            }
          }
        }

        stage('Frontend') {
          agent { label 'frontend' }
          stages {
            stage('Frontend tests') {
              steps {
                dir('frontend') {
                  sh 'npm ci'
                  sh 'npx ng test --watch=false --coverage --coverage-reporters=lcov --reporters=junit --reporters=default --output-file=test-results/junit.xml'
                }
              }
              post {
                always {
                  junit allowEmptyResults: true, testResults: 'frontend/test-results/junit.xml'
                }
              }
            }

            stage('Frontend analysis') {
              steps {
                fetchTargetBranch()
                dir('frontend') {
                  withSonarQubeEnv('sonarqube') {
                    sh '''
                      npx @sonar/scan \
                        -Dsonar.projectKey=buy-02-frontend \
                        "-Dsonar.projectName=buy-02 frontend" \
                        -Dsonar.sources=src \
                        -Dsonar.tests=src \
                        "-Dsonar.exclusions=**/*.spec.ts" \
                        "-Dsonar.test.inclusions=**/*.spec.ts" \
                        -Dsonar.javascript.lcov.reportPaths=coverage/frontend/lcov.info \
                        -Dsonar.qualitygate.wait=true \
                        -Dsonar.qualitygate.timeout=300 \
                        $SONAR_ARGS
                    '''
                  }
                }
              }
            }
          }
        }
      }
    }

    // Runs only for pushes to main, and only if tests and quality gates passed.
    // Both stages share one agent and checkout.
    stage('Deliver') {
      when {
        branch 'main'
        not { triggeredBy 'TimerTrigger' }   // the nightly run tests and analyses, but doesn't redeploy
        beforeAgent true                     // check first, so PR and nightly builds don't take the backend agent
      }
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
                for svc in $BACKEND_SVCS frontend; do
                  docker tag ecommerce/$svc:$TAG ecommerce/$svc:last-good
                done
                docker images 'ecommerce/*' --format '{{.Repository}}:{{.Tag}}' \
                  | grep -vE ":($TAG|last-good)$" \
                  | xargs -r docker rmi
                docker image prune -f
              '''
              script { env.DEPLOYED = 'true' }   // reported in the email
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
        subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}" + (env.DEPLOYED ? ' deployed' : ' (tests and analysis passed)'),
        body: """Build #${env.BUILD_NUMBER} of ${env.BRANCH_NAME} passed all tests and quality gates.
          ${env.DEPLOYED ? "Deployed. Running images: ecommerce/*:${env.TAG}" : 'Not deployed: only pushes to main deploy (PR builds and the nightly scan do not).'}

          Changes: ${env.BUILD_URL}changes
          Console: ${env.BUILD_URL}console
        """
      )
    }
    failure {
      emailext(
        to: '$DEFAULT_RECIPIENTS',
        subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}" + (env.ROLLED_BACK ? ' (rolled back)' : ''),
        body: """Build #${env.BUILD_NUMBER} of ${env.BRANCH_NAME} failed.
          ${env.ROLLED_BACK ? 'The new release was unhealthy, so the previous good release (last-good) was redeployed. The site is still up.' : 'Nothing new was deployed: the running release is unchanged.'}

          Test results: ${env.BUILD_URL}testReport
          Console: ${env.BUILD_URL}console
        """
      )
    }
  }
}

def fetchTargetBranch() {
  if (env.CHANGE_ID) {
    withCredentials([gitUsernamePassword(credentialsId: 'git-token')]) {
      sh 'git fetch --no-tags origin "+refs/heads/$CHANGE_TARGET:refs/remotes/origin/$CHANGE_TARGET"'
    }
  }
}