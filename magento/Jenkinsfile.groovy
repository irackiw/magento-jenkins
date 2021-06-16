package magento

RELEASE_DIR = getReleaseDir()
RELEASE_TMP_PATH =  "/tmp/" + RELEASE_DIR
RELEASE_COMPRESS = RELEASE_DIR + ".tar.gz"
RELEASE_TMP_COMPRESS_PATH = "/tmp/" + RELEASE_COMPRESS
SYMLINK_PATH = "/var/www/current_magento"


PRAPS_IP = ['75.119.139.222'];
PRAP_USER = 'ecom'

pipeline {
    agent any

    parameters {
        string(
                defaultValue: "https://github.com/irackiw/magento.git",
                description: "Repository",
                name: "repo"
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
                    sh "git clone -b ${params.branch} ${params.repo} ${RELEASE_TMP_PATH}"
                }
            }
        }
        stage('Composer install') {
            steps {
                script {
                    sh "cd ${RELEASE_TMP_PATH} && composer install"
                    sh "rm -rf ${RELEASE_TMP_PATH}/var/.regenerate"
                }
            }
        }
//        stage('Fix bin/magento permissions') {
//            steps {
//                script {
//                    sh "chmod u+x ${RELEASE_TMP_PATH}/bin/magento"
//                }
//            }
//        }

        stage('Setup upgrade') {
            steps {
                script {
                    sh "${RELEASE_TMP_PATH}/bin/magento setup:upgrade"
                }
            }
        }
        stage('DI compile') {
            steps {
                sh("${RELEASE_TMP_PATH}/bin/magento setup:di:compile")
            }
        }
        stage('Static content deploy') {
            steps {
                sh("${RELEASE_TMP_PATH}/bin/magento setup:static-content:deploy pl_PL -f")
            }
        }
        stage('Compress release') {
            steps {
                sh("cd /tmp && tar -zcvf $RELEASE_DIR'.tar.gz' $RELEASE_DIR")
            }
        }
        stage('Upload & unzip code to praps') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        println("Upload code for $prapIp")
                        sh("scp ${RELEASE_TMP_COMPRESS_PATH} $PRAP_USER@$prapIp:/var/www/magento_versions/${RELEASE_COMPRESS}")
                        sh("ssh $PRAP_USER@$prapIp 'cd /var/www/magento_versions && tar -zxvf ${RELEASE_COMPRESS}'")

                    }
                    println("Code uploaded for all praps")
                }
            }
        }
        stage('Change symlinks') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp 'ln -sf /var/www/magento_versions/${RELEASE_DIR}/ /var/www/current_magento'"
                    }
                }
            }
        }
//        stage('Fix permissions') {
//            steps {
//                script {
//                    for (prapIp in PRAPS_IP) {
//                        sh "ssh $PRAP_USER@$prapIp && sudo /usr/local/bin/fix_permissions.sh ${RELEASE_DIR}"
//                    }
//                }
//            }
//        }
        stage('Magento cache clear') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp  '/var/www/current_magento/bin/magento cache:clean && /var/www/current_magento/bin/magento cache:enable'"
                    }
                }
            }
        }
        stage('Symlink uploads') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp 'ln -sf /mnt/s3/shared_magento/media /var/www/current_magento/pub/media'"
                    }
                }
            }
        }
        stage('Remove old versions ') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp 'cd /var/www/magento_versions && rm -rf ${RELEASE_COMPRESS}'"
                        sh "ssh $PRAP_USER@$prapIp 'cd /var/www/magento_versions && ls -t | tail -n +4 | xargs -I {} rm -rf {}'"
                    }
                }
            }
        }
        stage('Clear locally /tmp') {
            steps {
                script {
                    sh "rm -rf $RELEASE_TMP_PATH"
                    sh "rm -rf $RELEASE_TMP_COMPRESS_PATH"
                }
            }
        }
    }
}


// magento/pub/media => s3
def getReleaseDir(){
    return "magetno_" + new Date().format("yyyyMMddhhmmss")
}

