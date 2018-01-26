def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    node('sonar') {
    stage('CodeAnalysis'){
        def BRANCH_NAME = "${env.BRANCH}"
        def PROJECT_CODE = "${config.PROJECT_CODE}"
        if (['stage', 'mss', 'ms-stage', 'ms-stg', 'msstage'].any { it == BRANCH_NAME }){
            stage('SCM') {
                dir("${env.WORKSPACE}") {
                    checkout([$class: 'GitSCM', branches: [[name: "${env.BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: "${config.GIT_URL}"]]])
                }
            }
            stage('SonarQube analysis') {
            // requires SonarQube Scanner 3.0+
	        sh "sed -ie \"s|_PROJECT_|$PROJECT_CODE|g\" ${env.WORKSPACE}/sonar-project.properties"
            def scannerHome = '/opt/sonar/bin/sonar-scanner';
                withSonarQubeEnv("${config.ServerSonar}") {
                    sh "${scannerHome}/bin/sonar-scanner -Dproject.settings=${env.WORKSPACE}/sonar-project.properties"
                }
            }
        }
    }
    }
}   
