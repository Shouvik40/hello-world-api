pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'hello-world-api'
        EC2_USER = 'ubuntu'
        SSH_KEY_PATH = "/var/lib/jenkins/workspace/key.pem"
        BUILD_TAG = "${DOCKER_IMAGE}:${BUILD_NUMBER}"  // Use BUILD_NUMBER to tag the Docker image
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout the code from GitHub
                git branch: 'main', url: 'https://github.com/Shouvik40/hello-world-api.git'
            }
        }

        stage('Clean Up') {
            steps {
                script {
                    // Clean up unused Docker images and containers to free up space
                    sh "docker system prune -af"
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Build and tag the Docker image with the build number
                    sh "pwd"
                    sh "docker build -t ${BUILD_TAG} ."
                    sh "docker save -o ${DOCKER_IMAGE}_${BUILD_NUMBER}.tar ${BUILD_TAG}"
                    sh "pwd"
                }
            }
        }

        stage('Deploy Docker Image to EC2') {
            steps {
                script {
                    // Specify the IP address of the EC2 instance
                    def ec2_ip = 'ec2-13-201-6-166.ap-south-1.compute.amazonaws.com'
                    
                    // Echo the IP being deployed to
                    echo "Deploying to EC2 instance with IP: ${ec2_ip}"
                    
                    // Copy the Docker image to the EC2 instance
                    sh "scp -i ${SSH_KEY_PATH} /var/lib/jenkins/workspace/hello-world-pipeline/${DOCKER_IMAGE}_${BUILD_NUMBER}.tar ${EC2_USER}@${ec2_ip}:/home/${EC2_USER}/"
                    
                    // Load and run the Docker image directly via ssh without using EOF
                    sh """
                    ssh -i ${SSH_KEY_PATH} ${EC2_USER}@${ec2_ip} \
                    'docker rm -f \$(docker ps -aq --filter name=${DOCKER_IMAGE}_${BUILD_NUMBER}) || true; \
                    docker rmi ${BUILD_TAG} || true; \
                    docker load -i /home/${EC2_USER}/${DOCKER_IMAGE}_${BUILD_NUMBER}.tar; \
                    docker run -d -p 3000:3000 --name ${DOCKER_IMAGE}_${BUILD_NUMBER} ${BUILD_TAG}'
                    """
                }
            }
        }
    }
}
