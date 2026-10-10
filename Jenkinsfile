pipeline {
  agent none   // every stage picks its own agent

  options {
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stage('Test') {
      parallel {
        stage('Backend tests') {
          agent { label 'backend' }
          environment {
            // Test-only values.
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
  }
}
