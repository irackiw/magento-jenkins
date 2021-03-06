package magento

RELEASE_DIR = getReleaseDir()
RELEASE_TMP_PATH =  "/tmp/" + RELEASE_DIR
RELEASE_COMPRESS = RELEASE_DIR + ".tar.gz"
RELEASE_TMP_COMPRESS_PATH = "/tmp/" + RELEASE_COMPRESS
SYMLINK_PATH = "/var/www/prestashop"

CHECK_SYMLINK = 'if [[ -L "$file" && -d "$file" ]] then unlink $SYMLINK_PATH fi'


PRAPS_IP = ['75.119.139.222'];
PRAP_USER = 'ecom'

pipeline {
    agent any

    parameters {
        string(
                defaultValue: "https://github.com/irackiw/prestashop.git",
                description: "Repository",
                name: "repo"
        )
        string(
                defaultValue: "main",
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
                }
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
                        sh("cd /tmp && scp ${RELEASE_COMPRESS} $PRAP_USER@$prapIp:/var/www/prestashop_versions/${RELEASE_COMPRESS}")
                        sh("ssh $PRAP_USER@$prapIp 'cd /var/www/prestashop_versions && tar -zxvf ${RELEASE_COMPRESS}'")

                    }
                    println("Code uploaded for all praps")
                }
            }
        }
        stage('Change symlinks') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp 'ln -sf /var/www/prestashop_versions/${RELEASE_DIR}/ /var/www/current_prestashop'"
                    }
                }
            }
        }
        stage('Symlink uploads') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp ln -s /mnt/s3/shared_prestashop/img /var/www/current_prestashop/img"
                    }
                }
            }
        }
        stage('Remove old versions ') {
            steps {
                script {
                    for (prapIp in PRAPS_IP) {
                        sh "ssh $PRAP_USER@$prapIp 'cd /var/www/prestashop_versions && rm -rf ${RELEASE_COMPRESS}'"
                        sh "ssh $PRAP_USER@$prapIp 'cd /var/www/prestashop_versions && ls -t | tail -n +4 | xargs -I {} rm -rf {}'"
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

def getReleaseDir(){
    return "prestashop_" + new Date().format("yyyyMMddhhmmss")
}

