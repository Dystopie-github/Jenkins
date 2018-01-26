/*
This job is to push code again Pantheon using their tag system.
The variable $PANTAG is used to send code to test or live on pantheon
*/
import java.text.*
def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
    
    //use this block to decide which pantheon tag to use between test and live
    def BRANCH_NAME = "${env.BRANCH}"
    def PANTAG = ''
    def NEWTAG = ''
    def TMPTAG = ''
    if (['uat', 'ms-uat', 'msuat'].any { it == BRANCH_NAME }){ PANTAG = "test" }
    if (['master'].any { it == BRANCH_NAME }){ PANTAG = "live" }
    
    // https://jenkins/view/MS/credentials/
    if ("${config.GIT_URL}".contains('gitlab')){ SSHAGENT = "" }
    if ("${config.GIT_URL}".contains('bitbucket')){ SSHAGENT = "" }

    stage('PausePingdom'){
        sh "curl -X PUT -u '${config.USERNAME}' -H 'Content-Type: application/json' -H 'App-Key: ${config.appkey}' -d 'paused=true' https://api.pingdom.com/api/2.0/checks/${config.PINGDOMCHECK}"
    }

	stage('cloneRepo'){
		dir("${env.WORKSPACE}"){
			checkout([$class: 'GitSCM', branches: [[name: "${env.BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$SSHAGENT", url: "${config.GIT_URL}"]]])
		}
	}
	stage('RemoveGitIgonre'){
        sh 'mv .pantheonignore .gitignore'
        sh 'git rm .pantheonignore'
    }
	stage('Composer'){
	   dir("${env.WORKSPACE}"){
	   if ("${config.COMPOSER}".contains('YES')){
               sh 'rm -rf vendor/'
               sh 'composer clear-cache'
               sh 'COMPOSER=composer.json composer install --prefer-dist --no-interaction --no-dev'
           }
	}
    stage('Gulp'){
       if ("${config.FE}".contains('YES')){
           dir("${env.WORKSPACE}/${config.GULP}"){
           sh 'npm install --loglevel error --silent'
           sh 'bower install'
           sh 'npm run build'
           }
           }
        }
    }
    stage('GitRemoteAddCommit'){
        sh "git remote add remoteClient ${config.CLIENTREPO}"
        sh 'git add -Af'
        sh 'git commit -m \"Push from Jenkins\"'
        sh 'git fetch remoteClient'
    }

    stage('createTag'){
        echo PANTAG
        TMPTAG = sh(script: "git tag | grep pantheon_${PANTAG}_ | sort -k1.15n | tail -1", returnStdout: true).trim()
        echo TMPTAG
        NEWTAG = "pantheon_${PANTAG}_" + (Integer.parseInt(TMPTAG.replaceAll("[^0-9]", "")) + 1)
        echo NEWTAG
    }
    stage('pushClient'){
        echo NEWTAG
        sshagent(["${config.CLIENTSSHAGENT}"]) {
           sh "git tag -a ${NEWTAG} -m \"Deploying from jenkins to Client\""
           sh "git push -u remoteClient ${NEWTAG}"
        }
    }
    stage('PausePingdom'){
        sh "curl -X PUT -u '${config.USERNAME}' -H 'Content-Type: application/json' -H 'App-Key: ${config.appkey}' -d 'paused=false' https://api.pingdom.com/api/2.0/checks/${config.PINGDOMCHECK}"
    }
	stage('CleanFolderEnd'){
		 deleteDir()
	}
    }
}
