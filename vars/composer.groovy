def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def SSHAGENT = ""
    node("${config.SERVER}") {
        if ("${config.GIT_URL}".contains('gitlab')){ SSHAGENT = "" }
        if ("${config.GIT_URL}".contains('bitbucket')){ SSHAGENT = "" }
        stage('GitCheckoutDrupal'){
            // Cloning the project repo inside the right folder
            dir("${env.WORKSPACE}") {
                // checkout the git
                checkout([$class: 'GitSCM', branches: [[name: "${config.BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$SSHAGENT", url: "${config.GIT_URL}"]]])
            }
        }
        stage('Composer'){
            dir("${env.WORKSPACE}${config.WWW}"){
                sh 'COMPOSER=composer.json composer install --no-interaction'
            }
        }
    }
}