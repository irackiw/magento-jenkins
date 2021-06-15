package magento

RELEASE_DIR = getReleaseDir()


pipeline {
    agent any

    parameters{
        string(
                defaultValue: "https://github.com/irackiw/prestashop",
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
                    sh "git clone git@github.com:irackiw/magento.git /var/www/versions/${RELEASE_DIR}"
                }
            }
        }
        stage('Composer install') {
            steps {
                script {
                    sh "cd /var/www/prestashop/versions/${RELEASE_DIR} && composer install"
                    sh "rm -rf /var/www/prestashop/versions/${RELEASE_DIR}/var/.regenerate"
                }
            }
        }
        stage('Change symlinks') {
            steps {
                sh "unlink /var/www/current_magento"
                sh "ln -s /var/www/magento/versions/${RELEASE_DIR}/ /var/www/current_magento"
            }
        }
        stage('Fix permissions') {
            steps {
                sh "sudo /usr/local/bin/fix_permissions.sh ${RELEASE_DIR}"
            }
        }
    }
}

def getReleaseDir() {
    return new Date().format("yyyyMMddhhmmss")
}
