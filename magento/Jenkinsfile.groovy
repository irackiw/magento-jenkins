package magento

RELEASE_DIR = getReleaseDir()
PRAPS_IP = ['75.119.139.212', '75.119.139.222'];
PRAP_USER = 'ecom'

pipeline {
    agent any

    parameters{
        string(
                defaultValue: "https://github.com/irackiw/magento.git",
                description: "Repository",
                name: "repository"
        )
        string(
                defaultValue: "master",
                description: "Branch",
                name: "branch"
        )
    }

    stages {
        stage('Pull new version') {
            steps {
                script {
                    sh "git clone -b ${params.branch} ${params.repo} ${RELEASE_DIR}"
                }
            }
        }
        stage('Composer install') {
            steps {
                script {
                    sh "cd ${RELEASE_DIR} && composer install"
                    sh "rm -rf ${RELEASE_DIR}/var/.regenerate"
                }
            }
        }
        stage('Fix bin/magento permissions') {
            steps {
                script {
                    sh "chmod u+x ${RELEASE_DIR}/bin/magento"
                }
            }
        }
//        stage('Copy .env file ') {
//            steps {
//                script {
//                    sh "rm /var/www/magento/versions/${RELEASE_DIR}/app/etc/env.php"
//                    sh "cp /var/www/env.php /var/www/magento/versions/${RELEASE_DIR}/app/etc"
//                }
//            }
//        }
        stage('Setup upgrade') {
            steps {
                script {
                    sh "${RELEASE_DIR}/bin/magento setup:upgrade"
                }
            }
        }
        stage('DI compile') {
            steps {
                sh("${RELEASE_DIR}/bin/magento setup:di:compile")
            }
        }
        stage('Static content deploy') {
            steps {
                sh("${RELEASE_DIR}/bin/magento setup:static-content:deploy pl_PL -f")
            }
        }
        stage('Upload code to praps') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        println("Upload code for $prapIp")
                        sh("scp ${RELEASE_DIR} jenkins@$prapIp:/var/www/magento/versions/${RELEASE_DIR}")
                    }
                    println("Code uploaded for all praps")
                }
            }
        }
        stage('Change symlinks') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp && unlink /var/www/current_magento"
                        sh "ssh $PRAP_USER@$prapIp && ln -s /var/www/magento/versions/${RELEASE_DIR}/ /var/www/current_magento"
                    }
                }
            }
        }
        stage('Fix permissions') {
            steps {
                script{
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp && sudo /usr/local/bin/fix_permissions.sh ${RELEASE_DIR}"
                    }
                }
            }
        }
        stage('Magento cache clear') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp && /var/www/current_magento/bin/magento cache:clean && /var/www/current_magento/bin/magento cache:enable"
                    }
                }
            }
        }
    }
}

def getReleaseDir() {
    return "/tmp/magetno_" + new Date().format("yyyyMMddhhmmss")
}
