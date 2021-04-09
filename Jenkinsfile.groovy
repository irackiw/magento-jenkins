pipeline {
    agent any


    stages {
        stage('Pull new version') {
            steps {
                script {
                    sh 'cd /var/www/versions  && git clone git@github.com:irackiw/magento.git terazzara'
                }
            }
        }
        stage('Composer install') {
            steps {
                script {
                    sh 'cd /var/www/versions/terazzara'
                    sh 'composer install'
                    sh 'rm -rf var/.regenerate'
                }
            }
        }
        stage('Fix permissions') {
            steps {
                script {
                    sh 'cd /var/www/versions/terazzara'
                    sh 'chmod u+x bin/magento'
                }
            }
        }
        stage('Setup upgrade') {
            steps {
                script {
                    sh 'cd /var/www/versions/terazzara'
                    sh 'bin/magento maintenance:enable'
                    sh 'bin/magento setup:upgrade'
                    sh 'bin/magento maintenance:disable'
                }
            }
        }
        stage('DI compile') {
            steps {
                sh 'cd /var/www/versions/terazzara'
                sh("bin/magento setup:di:compile")
            }
        }
        stage('Static content deploy') {
            steps {
                sh 'cd /var/www/versions/terazzara'
                sh("bin/magento setup:static-content:deploy")
            }
        }
        stage('Change symlinks') {
            steps {
                sh("ls -l ")
            }
        }
        stage('Magento cache clear') {
            steps {
                sh("bin/magento cache:clean")
                sh("bin/magento cache:enable")
            }
        }
        stage('Cloudflare cache clear') {
            steps {
                echo 'Deploying....'
            }
        }
    }
    post {
        always {
            script{
                sh 'rm -rf /var/www/versions/terazzara'
            }
        }
    }
}

def getReleaseDir() {
    return new Date().format('yyyyMMddHHss')
}
