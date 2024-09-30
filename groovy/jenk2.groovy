pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'hello-world-api'
        EC2_USER = 'ubuntu'
        SSH_KEY_PATH = "/var/lib/jenkins/workspace/key.pem"
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
                    // Build and save the Docker image
                    sh "docker build -t ${DOCKER_IMAGE} ."
                    sh "docker save -o ${DOCKER_IMAGE}.tar ${DOCKER_IMAGE}"
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
                    
                    // Copy Docker image to EC2 instance and run the container
                    sh """
                    scp -i ${SSH_KEY_PATH}  /var/lib/jenkins/workspace/hello-world-pipeline/hello-world-api.tar ubuntu@ec2-13-201-6-166.ap-south-1.compute.amazonaws.com:/home/ubuntu/
                    ssh -i ${SSH_KEY_PATH} ${EC2_USER}@${ec2_ip} << 'EOF'
                        docker rm -f \$(docker ps -aq --filter name=${DOCKER_IMAGE}) || true
                        docker rmi ${DOCKER_IMAGE}:latest || true
                        docker load -i /home/${EC2_USER}/${DOCKER_IMAGE}.tar
                        docker run -d -p 3000:3000 --name ${DOCKER_IMAGE} ${DOCKER_IMAGE}:latest
                    EOF
                    """
                }
            }
        }
    }
}
