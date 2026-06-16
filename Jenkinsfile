// ================================================================
// JENKINSFILE - Shranvi Spring Boot API - Enterprise CI/CD Pipeline
// Flow: GitHub Push → Build → Test → Quality → Docker → Deploy
// Target Server: EC2 instance | App Port: 8081 (Flask runs on 5000)
// ================================================================

pipeline {

    agent any

    environment {
        APP_NAME        = 'shranvi-products-api'
        APP_VERSION     = "${BUILD_NUMBER}"
        APP_PORT        = '8081'

        DOCKER_IMAGE    = "ashish6310/shranvi-products-api"
        DOCKER_TAG      = "${BUILD_NUMBER}"

        DOCKER_CREDENTIALS  = 'dockerhub-creds'
        EC2_SSH_KEY         = 'ec2-ssh-private-key'
        SONAR_TOKEN         = 'sonarqube-token'

        EC2_HOST        = '13.201.74.107'
        EC2_USER        = 'ubuntu'
        DEPLOY_DIR      = '/opt/shranvi-api'

        SONAR_HOST      = 'http://localhost:9000'
    }

    triggers {
        githubPush()
        cron('H 2 * * *')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
        ansiColor('xterm')
    }

    stages {

        stage('📥 Checkout Code') {
            steps {
                echo '=== STAGE: Checkout ==='
                cleanWs()
                checkout scm
                sh '''
                    echo "Branch: $(git branch --show-current)"
                    echo "Commit: $(git log --oneline -1)"
                    echo "Author: $(git log --format='%an' -1)"
                '''
            }
            post {
                failure { echo '❌ STAGE FAILED: Checkout — could not pull source from GitHub. Check repo URL/credentials.' }
            }
        }

        stage('🔨 Build Application') {
            steps {
                echo '=== STAGE: Build (Maven compile) ==='
                script {
                    try {
                        sh 'mvn clean compile -B -DskipTests'
                        echo '✅ Build successful!'
                    } catch (err) {
                        echo "❌ STAGE FAILED: Build — Maven compilation error.\nReason: ${err.getMessage()}"
                        error("Build stage failed: ${err.getMessage()}")
                    }
                }
            }
        }

        stage('🧪 Unit Tests') {
            steps {
                echo '=== STAGE: Unit Tests (JUnit + Mockito) ==='
                script {
                    try {
                        sh 'mvn test -B'
                        echo '✅ All tests passed!'
                    } catch (err) {
                        echo "❌ STAGE FAILED: Unit Tests — one or more tests failed.\nReason: ${err.getMessage()}"
                        error("Unit Tests stage failed: ${err.getMessage()}")
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        minimumInstructionCoverage: '70',
                        minimumLineCoverage: '70'
                    )
                }
            }
        }

        stage('📊 Code Quality (SonarQube)') {
            steps {
                echo '=== STAGE: SonarQube Analysis ==='
                script {
                    try {
                        withCredentials([string(credentialsId: env.SONAR_TOKEN, variable: 'SONAR_AUTH_TOKEN')]) {
                            sh '''
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=${APP_NAME} \
                                    -Dsonar.host.url=${SONAR_HOST} \
                                    -Dsonar.login=${SONAR_AUTH_TOKEN} \
                                    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                    -B
                            '''
                        }
                        echo '✅ SonarQube analysis complete!'
                    } catch (err) {
                        echo "⚠️ STAGE WARNING: SonarQube analysis failed (non-blocking).\nReason: ${err.getMessage()}"
                        unstable("SonarQube analysis failed: ${err.getMessage()}")
                    }
                }
            }
        }

        stage('📦 Package JAR') {
            steps {
                echo '=== STAGE: Package JAR ==='
                script {
                    try {
                        sh '''
                            mvn package -DskipTests -B
                            ls -lh target/*.jar
                        '''
                        echo '✅ JAR created successfully!'
                    } catch (err) {
                        echo "❌ STAGE FAILED: Package — JAR packaging failed.\nReason: ${err.getMessage()}"
                        error("Package stage failed: ${err.getMessage()}")
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: false
                }
            }
        }

        stage('🐳 Docker Build') {
            steps {
                echo '=== STAGE: Docker Build ==='
                script {
                    try {
                        sh '''
                            docker build \
                                -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                                -t ${DOCKER_IMAGE}:latest \
                                --label "build=${BUILD_NUMBER}" \
                                --label "commit=$(git log --format='%H' -1 | cut -c1-8)" \
                                .
                            docker images | grep shranvi
                        '''
                        echo "✅ Docker image built: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    } catch (err) {
                        echo "❌ STAGE FAILED: Docker Build — image build failed (check Dockerfile/context).\nReason: ${err.getMessage()}"
                        error("Docker Build stage failed: ${err.getMessage()}")
                    }
                }
            }
        }

        stage('📤 Push to Docker Hub') {
            steps {
                echo '=== STAGE: Push to Docker Hub ==='
                script {
                    try {
                        withCredentials([usernamePassword(
                            credentialsId: env.DOCKER_CREDENTIALS,
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )]) {
                            sh '''
                                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                                docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                                docker push ${DOCKER_IMAGE}:latest
                                docker logout
                            '''
                        }
                        echo '✅ Image pushed to Docker Hub!'
                    } catch (err) {
                        echo "❌ STAGE FAILED: Docker Push — could not push image (check DockerHub creds/network).\nReason: ${err.getMessage()}"
                        error("Docker Push stage failed: ${err.getMessage()}")
                    }
                }
            }
        }

        // ----------------------------------------------------------
        // STAGE 9: DEPLOY TO EC2 — full error visibility
        // ----------------------------------------------------------
        stage('🚀 Deploy to EC2') {
            steps {
                echo '=== STAGE: Deploy to EC2 ==='
                script {
                    try {
                        withCredentials([sshUserPrivateKey(
                            credentialsId: env.EC2_SSH_KEY,
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        )]) {

                            // Step 1: Test SSH connectivity first, fail fast with a clear reason
                            sh '''
                                set -e
                                echo "🔍 Testing SSH connectivity to ${EC2_HOST}..."
                                ssh -i $SSH_KEY -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                                    ${EC2_USER}@${EC2_HOST} "echo '✅ SSH connection OK'" \
                                    || { echo "❌ SSH CONNECTION FAILED to ${EC2_HOST}. Check security group, key, or instance state."; exit 1; }
                            '''

                            // Step 2: Copy deployment file
                            sh '''
                                set -e
                                echo "📤 Copying docker-compose.yml to EC2..."
                                scp -i $SSH_KEY -o StrictHostKeyChecking=no \
                                    docker-compose.yml \
                                    ${EC2_USER}@${EC2_HOST}:${DEPLOY_DIR}/ \
                                    || { echo "❌ SCP FAILED. Check DEPLOY_DIR exists and permissions on EC2 (${DEPLOY_DIR})."; exit 1; }
                            '''

                            // Step 3: Remote deploy script with set -ex so EVERY command and its
                            // exit status is printed in Jenkins console — this is what was missing before.
                            sh '''
                                set -e
                                ssh -i $SSH_KEY -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} \
                                    "DOCKER_IMAGE=${DOCKER_IMAGE} DEPLOY_DIR=${DEPLOY_DIR} bash -s" << 'ENDSSH'
set -ex   # -e: exit on error, -x: print each command (THIS is what surfaces hidden errors)

echo "📂 Moving to deployment directory..."
cd "${DEPLOY_DIR}" || { echo "❌ DEPLOY_DIR ${DEPLOY_DIR} not found on EC2"; exit 1; }

echo "📥 Pulling latest Docker image: ${DOCKER_IMAGE}:latest"
docker pull "${DOCKER_IMAGE}:latest" \
    || { echo "❌ DOCKER PULL FAILED. Check image name/tag and DockerHub auth on EC2."; exit 1; }

echo "🛑 Stopping old container (if running)..."
docker stop shranvi-api 2>/dev/null || echo "ℹ️ No running container named shranvi-api"
docker rm shranvi-api 2>/dev/null || echo "ℹ️ No old container to remove"

echo "▶️ Starting new container on port 8081..."
docker run -d \
    --name shranvi-api \
    --restart unless-stopped \
    -p 8081:8081 \
    -e SPRING_PROFILES_ACTIVE=production \
    -v /var/log/shranvi-api:/var/log/shranvi-api \
    "${DOCKER_IMAGE}:latest" \
    || { echo "❌ DOCKER RUN FAILED. Port 8081 may be in use, or image is broken."; docker logs shranvi-api 2>&1 || true; exit 1; }

echo "⏳ Waiting for app to start (30 seconds)..."
sleep 30

echo "📋 Container status:"
docker ps -a | grep shranvi-api || true

echo "📜 Last 50 lines of container logs:"
docker logs --tail 50 shranvi-api || true

echo "🏥 Running health check..."
HEALTH_CODE=$(curl -s -o /tmp/health_response.txt -w "%{http_code}" http://localhost:8081/api/v1/products/health || echo "000")
echo "Health check HTTP status: ${HEALTH_CODE}"
echo "Health check response body:"
cat /tmp/health_response.txt 2>/dev/null || echo "(no response body)"

if [ "${HEALTH_CODE}" != "200" ]; then
    echo "❌ HEALTH CHECK FAILED with status ${HEALTH_CODE}. Dumping full container logs:"
    docker logs shranvi-api || true
    exit 1
fi

echo "✅ Deployment SUCCESSFUL! App running on port 8081"
ENDSSH
                            '''
                        }
                        echo '✅ EC2 deployment completed successfully!'

                    } catch (err) {
                        // This block guarantees the real reason is printed in Jenkins,
                        // instead of a generic "script returned exit code 1"
                        echo "❌ STAGE FAILED: Deploy to EC2"
                        echo "❌ Error details: ${err.getMessage()}"
                        echo "👉 Common causes: SSH/network issue, DEPLOY_DIR missing, port 8081 already in use, app crash on startup, or health endpoint not responding."
                        echo "👉 Check the 'docker logs shranvi-api' output above (if printed) for the app-level stack trace."
                        currentBuild.result = 'FAILURE'
                        error("EC2 Deployment failed: ${err.getMessage()}")
                    }
                }
            }
            post {
                failure {
                    echo '🔎 Attempting to fetch remote container logs for diagnosis (best effort)...'
                    script {
                        try {
                            withCredentials([sshUserPrivateKey(
                                credentialsId: env.EC2_SSH_KEY,
                                keyFileVariable: 'SSH_KEY',
                                usernameVariable: 'SSH_USER'
                            )]) {
                                sh '''
                                    ssh -i $SSH_KEY -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                                        ${EC2_USER}@${EC2_HOST} \
                                        "echo '--- docker ps -a ---'; docker ps -a | grep shranvi-api || true; echo '--- last 100 log lines ---'; docker logs --tail 100 shranvi-api 2>&1 || true" \
                                        || echo "⚠️ Could not retrieve diagnostic logs from EC2 (connection itself may be down)."
                                '''
                            }
                        } catch (diagErr) {
                            echo "⚠️ Diagnostic log fetch also failed: ${diagErr.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('🔥 Smoke Test') {
            steps {
                echo '=== STAGE: Smoke Test ==='
                script {
                    try {
                        sh '''
                            sleep 10
                            HEALTH=$(curl -s -o /tmp/smoke_health.txt -w "%{http_code}" \
                                http://${EC2_HOST}:${APP_PORT}/api/v1/products/health)
                            echo "Health endpoint status: $HEALTH"
                            cat /tmp/smoke_health.txt 2>/dev/null || true

                            if [ "$HEALTH" != "200" ]; then
                                echo "❌ Smoke test FAILED! HTTP status: $HEALTH"
                                exit 1
                            fi
                            echo "✅ Smoke test PASSED! API is responding on port ${APP_PORT}"

                            PRODUCTS=$(curl -s -o /tmp/smoke_products.txt -w "%{http_code}" \
                                http://${EC2_HOST}:${APP_PORT}/api/v1/products)
                            echo "Products API status: $PRODUCTS"
                        '''
                    } catch (err) {
                        echo "❌ STAGE FAILED: Smoke Test — deployed app is not responding correctly.\nReason: ${err.getMessage()}"
                        error("Smoke Test stage failed: ${err.getMessage()}")
                    }
                }
            }
        }
    }

    post {
        always {
            echo '=== Pipeline finished. Cleaning up local Docker images ==='
            sh '''
                docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} 2>/dev/null || true
                docker image prune -f 2>/dev/null || true
            '''
        }

        success {
            echo """
            ╔══════════════════════════════════════════╗
            ║  ✅ DEPLOYMENT SUCCESSFUL!                ║
            ║  App: ${APP_NAME}                         ║
            ║  Build: #${BUILD_NUMBER}                  ║
            ║  URL: http://${EC2_HOST}:8081             ║
            ╚══════════════════════════════════════════╝
            """
        }

        failure {
            echo """
            ╔══════════════════════════════════════════╗
            ║  ❌ PIPELINE FAILED!                      ║
            ║  Build: #${BUILD_NUMBER}                  ║
            ║  Failed Stage: ${env.STAGE_NAME ?: 'unknown'}
            ║  Check console log above for ❌ markers   ║
            ╚══════════════════════════════════════════╝
            """
        }

        unstable {
            echo '⚠️ Pipeline is UNSTABLE - SonarQube or some checks may have failed (non-blocking).'
        }
    }
}