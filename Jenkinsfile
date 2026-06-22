pipeline {

agent any

environment {
    APP_NAME = "shranvi-api"
    IMAGE_NAME = "743320494757.dkr.ecr.ap-south-1.amazonaws.com/shranvi"
    IMAGE_TAG = "${BUILD_NUMBER}"

    SWARM_MANAGER = "13.203.205.39"
    SSH_USER = "ubuntu"

    AWS_REGION = "ap-south-1"
}

options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timeout(time: 30, unit: 'MINUTES')
}

stages {

    stage('Checkout') {
        steps {
            echo "Checking out source code..."
            checkout scm
        }
    }

    stage('Build') {
        steps {
            echo "Building application..."
            sh 'mvn clean package -DskipTests'
        }
    }

    stage('Unit Tests') {
        steps {
            echo "Running tests..."
            sh 'mvn test'
        }
    }

    stage('Docker Build') {
        steps {
            sh """
                docker build \
                -t ${IMAGE_NAME}:${IMAGE_TAG} \
                -t ${IMAGE_NAME}:latest .
            """
        }
    }

    stage('Push Image To Registry') {
        steps {

            withCredentials([
                usernamePassword(
                    credentialsId: 'aws-ecr-creds',
                    usernameVariable: 'AWS_ACCESS_KEY_ID',
                    passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                )
            ]) {

                sh '''
                    export AWS_DEFAULT_REGION=${AWS_REGION}

                    aws ecr get-login-password \
                    --region ${AWS_REGION} \
                    | docker login \
                    --username AWS \
                    --password-stdin \
                    743320494757.dkr.ecr.ap-south-1.amazonaws.com

                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${IMAGE_NAME}:latest
                '''
            }
        }
    }

    stage('Docker Swarm Canary Deployment') {

        steps {

            withCredentials([
                sshUserPrivateKey(
                    credentialsId: 'ec2-ssh-key',
                    keyFileVariable: 'SSH_KEY'
                ),
                usernamePassword(
                    credentialsId: 'aws-ecr-creds',
                    usernameVariable: 'AWS_ACCESS_KEY_ID',
                    passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                )
            ]) {

                sh '''
                ssh -o StrictHostKeyChecking=no \
                -i ${SSH_KEY} \
                ${SSH_USER}@${SWARM_MANAGER} "

                export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                export AWS_DEFAULT_REGION=${AWS_REGION}

                aws ecr get-login-password \
                --region ${AWS_REGION} \
                | docker login \
                --username AWS \
                --password-stdin \
                743320494757.dkr.ecr.ap-south-1.amazonaws.com

                docker pull ${IMAGE_NAME}:${IMAGE_TAG}

                docker service update \
                  --image ${IMAGE_NAME}:${IMAGE_TAG} \
                  --update-parallelism 1 \
                  --update-delay 30s \
                  --update-order start-first \
                  ${APP_NAME}
                "
                '''
            }
        }
    }

    stage('Verify Deployment') {

        steps {

            withCredentials([
                sshUserPrivateKey(
                    credentialsId: 'ec2-ssh-key',
                    keyFileVariable: 'SSH_KEY'
                )
            ]) {

                sh '''
                ssh -o StrictHostKeyChecking=no \
                -i ${SSH_KEY} \
                ${SSH_USER}@${SWARM_MANAGER} "

                docker service ls

                docker service ps ${APP_NAME}

                docker service inspect ${APP_NAME}
                "
                '''
            }
        }
    }

    stage('Health Check') {

        steps {

            sleep 30

            script {

                def status = sh(
                    script: '''
                        curl -s -o /dev/null \
                        -w "%{http_code}" \
                        http://YOUR_LOAD_BALANCER_OR_SWARM_IP:8081/api/v1/products/health
                    ''',
                    returnStdout: true
                ).trim()

                if (status != "200") {
                    error("Health check failed")
                }
            }
        }
    }
}

post {

    success {
        echo "Deployment Successful"
    }

    failure {

        echo "Deployment Failed"

        withCredentials([
            sshUserPrivateKey(
                credentialsId: 'ec2-ssh-key',
                keyFileVariable: 'SSH_KEY'
            )
        ]) {

            sh '''
            ssh -o StrictHostKeyChecking=no \
            -i ${SSH_KEY} \
            ${SSH_USER}@${SWARM_MANAGER} "

            docker service rollback ${APP_NAME}
            "
            '''
        }
    }

    always {

        sh '''
            docker image prune -f || true
        '''
    }
}

}
