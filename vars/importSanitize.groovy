def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
        def SSH_SERVER = "${config.SSH_SERVER}"
        def files = "$BUILD_NUMBER\".tar.gz\""
        if ("${config.GIT_URL}".contains('gitlab')){ SSHAGENT = "" }
        if ("${config.GIT_URL}".contains('bitbucket')){ SSHAGENT = "" }
        stage('GitCheckoutDrupal'){
            // Cloning the project repo inside the right folder
            dir("${env.WORKSPACE}") {
                // checkout the git
                checkout([$class: 'GitSCM', branches: [[name: "${env.BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$SSHAGENT", url: "${config.GIT_URL}"]]])
            }
        }
        stage ('DrushArr'){
            dir('/home/jenkins/workspace/website/'){
                sh "drush arr ${files} --db-url=mysql://root:password@127.0.0.1/${config.PROJECT_NAME}"
            }
        }
        stage ('sanitize'){
            sh "mysql -h localhost -u root -password ${config.PROJECT_NAME} < sanitize.sql"
        }
    }
}