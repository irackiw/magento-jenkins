RELEASE_DIR = getReleaseDir()


pipeline {
    agent any


    stages {
        stage('Pull new version') {
            steps {
                script {
                    sh "git clone git@github.com:irackiw/magento.git /var/www/versions/${RELEASE_DIR}"
                }
            }
        }
        stage('Composer install') {
            steps {
                script {
                    sh "cd /var/www/versions/${RELEASE_DIR} && composer install"
                    sh "rm -rf /var/www/versions/${RELEASE_DIR}/var/.regenerate"
                }
            }
        }
        stage('Fix bin/magento permissions') {
            steps {
                script {
                    sh "chmod u+x /var/www/versions/${RELEASE_DIR}/bin/magento"
                }
            }
        }
        stage('Copy .env file ') {
            steps {
                script {
                    sh "rm /var/www/versions/${RELEASE_DIR}/app/etc/env.php"
                    sh "cp /var/www/env.php /var/www/versions/${RELEASE_DIR}/app/etc"
                }
            }
        }
        stage('Setup upgrade') {
            steps {
                script {
                    sh "/var/www/versions/${RELEASE_DIR}/bin/magento setup:upgrade"
                }
            }
        }
        stage('DI compile') {
            steps {
                sh("/var/www/versions/${RELEASE_DIR}/bin/magento setup:di:compile")
            }
        }
        stage('Static content deploy') {
            steps {
                sh("/var/www/versions/${RELEASE_DIR}/bin/magento setup:static-content:deploy -f")
            }
        }
        stage('Change symlinks') {
            steps {
                sh "if [- f '/var/www/current_magento' ] ; then unlink /var/www/current_magento; fi"
                sh "ln -s /var/www/versions/${RELEASE_DIR} /var/www/current_magento"
            }
        }
        stage('Magento cache clear') {
            steps {
                sh("/var/www/current_magento/bin/magento cache:clean")
                sh("/var/www/current_magento/bin/magento cache:enable")
            }
        }
        stage('Cloudflare cache clear') {
            steps {
                echo 'Deploying finished.'
            }
        }
    }
}

def getReleaseDir() {
    return new Date().format("yyyyMMddhhmmss")
}
