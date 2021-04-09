RELEASE_DIR = getReleaseDir()


pipeline {
    agent any


    stages {
        stage('Pull new version') {
            steps {
                script {


                                                            \



                    echo 'wojtek'
                    echo RELEASE_DIR
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
        stage('Fix permissions') {
            steps {
                script {
                    sh "chmod u+x /var/www/versions/${RELEASE_DIR}/bin/magento"
                }
            }
        }
        stage('Setup upgrade') {
            steps {
                script {
                    sh "/var/www/versions/${RELEASE_DIR}/bin/magento maintenance:enable"
                    sh "/var/www/versions/${RELEASE_DIR}/bin/magento setup:upgrade"
                    sh "/var/www/versions/${RELEASE_DIR}/bin/magento maintenance:disable"
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
                sh("/var/www/versions/${RELEASE_DIR}/bin/magento setup:static-content:deploy")
            }
        }
        stage('Change symlinks') {
            steps {
                sh "unlink /var/www/current_magento"
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
