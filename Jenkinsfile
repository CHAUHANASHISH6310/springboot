// ================================================================
// JENKINSFILE - Shranvi Spring Boot API - Enterprise CI/CD Pipeline
// 
// Flow: GitHub Push → Build → Test → Quality → Security → Docker → Deploy
// Target Server: EC2 instance
// App Port: 8081 (Flask runs on 5000)
// ================================================================

pipeline {

    // Run on any available Jenkins agent (build server)
    agent any

    // ============================================================
    // ENVIRONMENT VARIABLES
    // These are like global variables used throughout the pipeline
    // ============================================================
    environment {
        // Application details
        APP_NAME        = 'shranvi-products-api'
        APP_VERSION     = "${BUILD_NUMBER}"         // Auto-increments on each build
        APP_PORT        = '8081'                    // Must NOT conflict with Flask (5000)

        // Docker Hub image name (change 'yourdockerhubusername' to yours)
        DOCKER_IMAGE    = "ashish6310/shranvi-products-api"
        DOCKER_TAG      = "${BUILD_NUMBER}"

        // Jenkins Credentials IDs (you set these in Jenkins > Manage Credentials)
        DOCKER_CREDENTIALS  = 'dockerhub-creds'  // DockerHub login
        EC2_SSH_KEY         = 'ec2-ssh-private-key'     // Your EC2 .pem key
        SONAR_TOKEN         = 'sonarqube-token'

        // EC2 Deployment Server
        EC2_HOST        = '13.201.XXX.XXX'          // ← PUT YOUR EC2 IP HERE
        EC2_USER        = 'ubuntu'                  // EC2 login user
        DEPLOY_DIR      = '/opt/shranvi-api'        // Deployment folder on EC2

        // SonarQube server (running on Jenkins server or separate server)
        SONAR_HOST      = 'http://localhost:9000'
    }

    // ============================================================
    // TRIGGERS - When should this pipeline auto-run?
    // ============================================================
    triggers {
        // Auto-trigger when GitHub sends a webhook push notification
        githubPush()
        
        // Also run at 2 AM every day (nightly build)
        cron('H 2 * * *')
    }

    // ============================================================
    // OPTIONS - Pipeline behavior settings
    // ============================================================
    options {
        // Keep only last 10 builds (saves disk space)
        buildDiscarder(logRotator(numToKeepStr: '10'))
        
        // Kill the pipeline if it runs more than 30 minutes
        timeout(time: 30, unit: 'MINUTES')
        
        // Don't run two builds of the same branch at the same time
        disableConcurrentBuilds()
        
        // Add timestamps in logs (easier debugging)
        timestamps()
    }

    // ============================================================
    // PIPELINE STAGES - Each block is one step in the pipeline
    // ============================================================
    stages {

        // ----------------------------------------------------------
        // STAGE 1: CHECKOUT
        // Jenkins downloads your code from GitHub
        // ----------------------------------------------------------
        stage('📥 Checkout Code') {
            steps {
                echo '=== Checking out source code from GitHub ==='
                
                // Clean workspace before checkout (fresh start)
                cleanWs()
                
                // Checkout the code that triggered this build
                checkout scm
                
                // Print what we checked out
                sh '''
                    echo "Branch: $(git branch --show-current)"
                    echo "Commit: $(git log --oneline -1)"
                    echo "Author: $(git log --format='%an' -1)"
                '''
            }
        }

        // ----------------------------------------------------------
        // STAGE 2: BUILD
        // Maven compiles your Java code into a JAR file
        // ----------------------------------------------------------
        stage('🔨 Build Application') {
            steps {
                echo '=== Building Spring Boot application with Maven ==='
                sh '''
                    # -B = batch mode (no interactive prompts)
                    # -DskipTests = skip tests here (we test in next stage separately)
                    mvn clean compile -B -DskipTests
                    echo "✅ Build successful!"
                '''
            }
            post {
                failure {
                    echo '❌ Build FAILED! Check your Java code for compilation errors.'
                }
            }
        }

        // ----------------------------------------------------------
        // STAGE 3: UNIT TESTING
        // Run all JUnit + Mockito tests
        // ----------------------------------------------------------
        stage('🧪 Unit Tests') {
            steps {
                echo '=== Running unit tests (JUnit + Mockito) ==='
                sh '''
                    # Run tests and generate JaCoCo coverage report
                    mvn test -B
                    echo "✅ All tests passed!"
                '''
            }
            post {
                always {
                    // Publish JUnit test results in Jenkins UI
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: false

                    // Publish JaCoCo code coverage report
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        minimumInstructionCoverage: '70',
                        minimumLineCoverage: '70'
                    )
                }
                failure {
                    echo '❌ Tests FAILED! Fix failing tests before merging.'
                }
            }
        }

        // ----------------------------------------------------------
        // STAGE 4: CODE QUALITY - SonarQube
        // Analyzes code for bugs, code smells, security issues
        // ----------------------------------------------------------
        stage('📊 Code Quality (SonarQube)') {
            steps {
                echo '=== Running SonarQube analysis ==='
                
                // Use SonarQube token from Jenkins credentials
                withCredentials([string(credentialsId: env.SONAR_TOKEN, variable: 'SONAR_AUTH_TOKEN')]) {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=${APP_NAME} \
                            -Dsonar.host.url=${SONAR_HOST} \
                            -Dsonar.login=${SONAR_AUTH_TOKEN} \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -B
                        echo "✅ SonarQube analysis complete - check dashboard!"
                    '''
                }
            }
            // Optional: Wait for SonarQube Quality Gate result
            // If quality gate fails, pipeline stops here
            // post {
            //     always {
            //         script {
            //             def qg = waitForQualityGate()
            //             if (qg.status != 'OK') {
            //                 error "Quality Gate failed: ${qg.status}"
            //             }
            //         }
            //     }
            // }
        }

        // ----------------------------------------------------------
        // STAGE 5: SECURITY SCAN - OWASP Dependency Check
        // Checks all your Maven libraries for known CVE vulnerabilities
        // ----------------------------------------------------------
        stage('🔒 Security Scan (OWASP)') {
            steps {
                echo '=== Scanning dependencies for CVE vulnerabilities ==='
                withCredentials([
                string(credentialsId: 'nvd-api-key',
                    variable: 'NVD_API_KEY')
            ]) {
                sh '''
                    mvn dependency-check:check \
                        -DnvdApiKey=$NVD_API_KEY \
                        -Dformat=HTML \
                        -B
                '''
                }
            }
            post {
                always {
                    // Publish the security report in Jenkins
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/dependency-check-report',
                        reportFiles: 'dependency-check-report.html',
                        reportName: 'OWASP Dependency Check Report'
                    ])
                }
                failure {
                    echo '❌ HIGH severity CVE found! Fix vulnerable dependencies.'
                }
            }
        }

        // ----------------------------------------------------------
        // STAGE 6: PACKAGE
        // Create the final executable JAR file
        // ----------------------------------------------------------
        stage('📦 Package JAR') {
            steps {
                echo '=== Packaging application into JAR ==='
                sh '''
                    # -DskipTests because we already tested above
                    mvn package -DskipTests -B
                    
                    # Show what was built
                    ls -lh target/*.jar
                    echo "✅ JAR created successfully!"
                '''
            }
            post {
                success {
                    // Archive the JAR so you can download it from Jenkins
                    archiveArtifacts artifacts: 'target/*.jar',
                                     fingerprint: true,
                                     allowEmptyArchive: false
                }
            }
        }

        // ----------------------------------------------------------
        // STAGE 7: DOCKER BUILD
        // Build Docker image from your Dockerfile
        // ----------------------------------------------------------
        stage('🐳 Docker Build') {
            steps {
                echo '=== Building Docker image ==='
                sh '''
                    # Build the image with two tags:
                    # 1. Build number tag (e.g., shranvi-products-api:42)
                    # 2. latest tag (always points to newest)
                    docker build \
                        -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                        -t ${DOCKER_IMAGE}:latest \
                        --label "build=${BUILD_NUMBER}" \
                        --label "commit=$(git log --format='%H' -1 | cut -c1-8)" \
                        .
                    
                    echo "✅ Docker image built: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    docker images | grep shranvi
                '''
            }
        }

        // ----------------------------------------------------------
        // STAGE 8: DOCKER PUSH
        // Push image to Docker Hub registry
        // ----------------------------------------------------------
        stage('📤 Push to Docker Hub') {
            steps {
                echo '=== Pushing Docker image to Docker Hub ==='
                
                // Login to Docker Hub using Jenkins stored credentials
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
                        echo "✅ Image pushed to Docker Hub!"
                    '''
                }
            }
        }

        // ----------------------------------------------------------
        // STAGE 9: DEPLOY TO EC2
        // SSH into EC2 server, pull new image, restart container
        // ----------------------------------------------------------
        stage('🚀 Deploy to EC2') {
            steps {
                echo '=== Deploying to EC2 server ==='
                
                // Use the EC2 SSH private key from Jenkins credentials
                withCredentials([sshUserPrivateKey(
                    credentialsId: env.EC2_SSH_KEY,
                    keyFileVariable: 'SSH_KEY',
                    usernameVariable: 'SSH_USER'
                )]) {
                    sh '''
                        # Copy deployment script to EC2
                        scp -i $SSH_KEY -o StrictHostKeyChecking=no \
                            docker-compose.yml \
                            ${EC2_USER}@${EC2_HOST}:${DEPLOY_DIR}/

                        # SSH into EC2 and run deployment commands
                        ssh -i $SSH_KEY -o StrictHostKeyChecking=no \
                            ${EC2_USER}@${EC2_HOST} << 'ENDSSH'
                            
                            set -e  # Exit on any error
                            
                            cd /opt/shranvi-api
                            
                            echo "📥 Pulling latest Docker image..."
                            docker pull ${DOCKER_IMAGE}:latest
                            
                            echo "🛑 Stopping old container (if running)..."
                            docker stop shranvi-api 2>/dev/null || true
                            docker rm shranvi-api 2>/dev/null || true
                            
                            echo "▶️ Starting new container on port 8081..."
                            docker run -d \
                                --name shranvi-api \
                                --restart unless-stopped \
                                -p 8081:8081 \
                                -e SPRING_PROFILES_ACTIVE=production \
                                -v /var/log/shranvi-api:/var/log/shranvi-api \
                                ${DOCKER_IMAGE}:latest
                            
                            echo "⏳ Waiting for app to start (30 seconds)..."
                            sleep 30
                            
                            echo "🏥 Health check..."
                            curl -f http://localhost:8081/api/v1/products/health || exit 1
                            
                            echo "✅ Deployment SUCCESSFUL! App running on port 8081"
                            docker ps | grep shranvi
ENDSSH
                    '''
                }
            }
        }

        // ----------------------------------------------------------
        // STAGE 10: SMOKE TEST
        // Quick test to verify deployment is working
        // ----------------------------------------------------------
        stage('🔥 Smoke Test') {
            steps {
                echo '=== Running smoke test on deployed application ==='
                sh '''
                    # Give the app a moment to fully start
                    sleep 10
                    
                    # Test health endpoint
                    HEALTH=$(curl -s -o /dev/null -w "%{http_code}" \
                        http://${EC2_HOST}:${APP_PORT}/api/v1/products/health)
                    
                    if [ "$HEALTH" = "200" ]; then
                        echo "✅ Smoke test PASSED! API is responding on port ${APP_PORT}"
                    else
                        echo "❌ Smoke test FAILED! HTTP status: $HEALTH"
                        exit 1
                    fi
                    
                    # Test products endpoint
                    PRODUCTS=$(curl -s -o /dev/null -w "%{http_code}" \
                        http://${EC2_HOST}:${APP_PORT}/api/v1/products)
                    echo "Products API status: $PRODUCTS"
                '''
            }
        }
    }

    // ============================================================
    // POST - Runs after all stages (success, failure, or always)
    // ============================================================
    post {
        // Always runs (cleanup)
        always {
            echo '=== Pipeline finished. Cleaning up local Docker images ==='
            sh '''
                docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} 2>/dev/null || true
                docker image prune -f 2>/dev/null || true
            '''
        }

        // Only runs on success
        success {
            echo """
            ╔══════════════════════════════════════════╗
            ║  ✅ DEPLOYMENT SUCCESSFUL!               ║
            ║  App: ${APP_NAME}                        ║
            ║  Build: #${BUILD_NUMBER}                 ║
            ║  URL: http://${EC2_HOST}:8081            ║
            ╚══════════════════════════════════════════╝
            """
            // TODO: Send Slack/Email notification on success
        }

        // Only runs on failure
        failure {
            echo """
            ╔══════════════════════════════════════════╗
            ║  ❌ PIPELINE FAILED!                     ║
            ║  Build: #${BUILD_NUMBER}                 ║
            ║  Check logs above for error details      ║
            ╚══════════════════════════════════════════╝
            """
            // TODO: Send Slack/Email alert on failure
        }

        // Only runs on unstable (tests failed but build succeeded)
        unstable {
            echo '⚠️ Pipeline is UNSTABLE - Some tests may have failed!'
        }
    }
}
