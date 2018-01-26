import java.text.*
def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
    
	def dateFormat = new SimpleDateFormat("yyyyMMdd")
	def date = new Date()
    def tagdate = dateFormat.format(date)
	def tag ="\"malagace\"-$tagdate-$BUILD_NUMBER"

    // https://jenkins/view/MS/credentials/
    if ("${config.GIT_URL}".contains('gitlab')){ SSHAGENT = "" }
    if ("${config.GIT_URL}".contains('bitbucket')){ SSHAGENT = "" }

	stage('CleanFolderEnd'){
		 deleteDir()
	}

    stage('PausePingdom'){
    sh "curl -X PUT -u '${config.UserAcount}' -H 'Content-Type: application/json' -H 'App-Key: ${config.APPKEY}' -d 'paused=true' https://api.pingdom.com/api/2.0/checks/${config.PINGDOMCHECK}"
    }
	stage('cloneRepo'){
		dir("${env.WORKSPACE}"){
			checkout([$class: 'GitSCM', branches: [[name: "${env.BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$SSHAGENT", url: "${config.GIT_UR}L"]]])
		}
	}
	stage('removeJenkinsfile'){
	    sh 'rm -f Jenkinsfile'
	}
	stage('ChangeGitignore'){
	     sh 'mv ${env.WORKSPACE}/.gitignore_hosting ${env.WORKSPACE}/.gitignore'
         sh 'git rm .gitignore_hosting'
	}
	stage('Composer'){
	   if (${config.COMPOSER}.contains('YES')){
	        sh 'rm -rf vendor '
	        sh 'composer clear-cache'
            sh 'composer install --prefer-dist --no-interaction --no-dev'
            stage('Gulp'){
                dir("${env.WORKSPACE}/${config.GULP}"){
                sh 'npm install --loglevel error --silent'
                sh 'bower install'
                sh 'npm run build'
                }
            }
           sh 'git add -Af'
           sh 'git commit -m \"Push from Jenkins\"'
        }
    }
    stage('Add remote repo'){
       sh "git remote add remoteClient ${config.CLIENTREPO}"
    }
	stage('createTag'){
        dir("${env.WORKSPACE}"){
            sh "git tag -a ${tag} -m \"Deploying from Jenkins to Client\""
		}
	}
	stage('pushClient'){
	    if ("${config.CLIENTREPO}".contains('Hosting')){
		sshagent(["$CLIENTSSHAGENT"]) {
			sh "git push origin master"
            sh "git push origin --tags"
		}
	    } else {
	        sshagent(["$CLIENTSSHAGENT"]) {
			sh "git push -u remoteClient ${tag}"
	        }
	    }
	    }
	stage('DrushHost3'){
	    if ("${config.CLIENTREPO}".contains('host3')){
	    def path; if (SSH_SERVER.contains('host3')){
	        path = sh (script: "ssh -tt ${config.SSH_SERVER} \"pwd\"", returnStdout: true).trim()
	    }
        sh "ssh -tt ${config.SSH_SERVER} \"cd $REMOTE_DOCROOT && drush updb ${config.MULTISITES} -y"
        sh "ssh -tt ${config.SSH_SERVER} \"cd $REMOTE_DOCROOT && drush cc ${config.MULTISITES} -y"
	    }
	}
    stage('PausePingdom'){
        sh "curl -X PUT -u '${config.UserAcount}' -H 'Content-Type: application/json' -H 'App-Key: ${config.APPKEY}' -d 'paused=false' https://api.pingdom.com/api/2.0/checks/${config.PINGDOMCHECK}"
    }
	stage('CleanFolderEnd'){
		 deleteDir()
	}
    }
}
