def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def err = ""
    node("${config.SERVER}") {
    // Use for error catching and email
    currentBuild.result = "SUCCESS"
    try {

    // https://jenkins/view/MS/credentials/
    if ("${config.GIT_URL}".contains('gitlab')){ SSHAGENT = "" }
    if ("${config.GIT_URL}".contains('bitbucket')){ SSHAGENT = "" }

    //use this block to create the .inc file according to the branch "type"
    def BRANCH_NAME = "${env.BRANCH}"
    def BRANCH_INC = "dev"
    if (['dev', 'msd', 'ms-dev', 'msdev'].any { it == BRANCH_NAME }){ BRANCH_INC = "dev" }
    if (['stage', 'mss', 'ms-stage', 'ms-stg', 'msstage'].any { it == BRANCH_NAME }){ BRANCH_INC = "stage" }
    if (['master'].any { it == BRANCH_NAME }){ BRANCH_INC = "prod" }

    // create the docroot folder for apache
    stage('CreateProjectFolder'){
        sh "sudo mkdir -p ${env.WORKSPACE}"
        sh "sudo mkdir -p ${env.WORKSPACE}/logs/"
        sh "sudo chown jenkins.${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
    }
    // Depend on server configurationa an hosting solution
    stage('WebDocroot'){
        if ("${config.PANTHEON}".contains('YES')){
            sh "ln -nsf ${env.WORKSPACE}/web ${env.WORKSPACE}/docroot"
        }
    }
    stage('CreateAccess'){ 
        sh "sudo mkdir -p ${env.WORKSPACE}/docroot/sites/default/files"
        sh "sudo chown jenkins.${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
        sh "sudo chmod -R 775 ${env.WORKSPACE}"
    }
    // Stage to clone the repo into the workspace
    stage('GitCheckoutDrupal'){
    // Cloning the project repo inside the right folder
        dir("${env.WORKSPACE}") {
            // checkout the git
            checkout([$class: 'GitSCM', branches: [[name: "${env.BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$SSHAGENT", url: "${config.GIT_URL}"]]])
        }
    }
	stage('Composer'){
	   dir("${env.WORKSPACE}"){
	   if ("${config.COMPOSER}".contains('YES')){
               sh 'rm -rf vendor/'
               sh 'COMPOSER=composer.json composer install --no-interaction'
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
    // Creating the httpd conf file for the website. The template file have is in the repo
    stage('HttpdConf'){
        sh "sudo cat /home/jenkins/templates/template.conf | sed \'s|{{ project }}|${config.PROJECT_NAME}|\' > ${env.WORKSPACE}/${config.PROJECT_NAME}.conf"
        sh returnStatus: true, script: "sudo ln -s ${env.WORKSPACE}/${config.PROJECT_NAME}.conf /etc/httpd/conf.d/${config.PROJECT_NAME}.conf"
        sh "sudo chown jenkins.${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
        sh returnStatus: true, script: 'sudo apachectl configtest && sudo apachectl restart'
     }
    // Create Database include file
    stage('CreateDatabaseFile'){
        FILE = "${env.WORKSPACE}/docroot/sites/default/"
        dir("$FILE") {
        writeFile file: "${BRANCH_INC}_settings.inc", text: """<?php
            \$databases[\'default\'][\'default\'] = array(
            \'driver\' => \'mysql\',
            \'database\' => \'${config.DB}\',
            \'username\' => \'${config.USERDB}\',
            \'password\' => \'${config.PASSDB}\',
            \'host\' => \'${config.SERVERDB}\',
            );
            ?>"""
        }
    }
    // Set the folder permission on the path for this node
    stage('SetPermission'){
        sh "sudo chown jenkins.${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
        sh "sudo chmod -R 755 ${env.WORKSPACE}/docroot/sites/default/"
        sh "sudo chmod -R 774 ${env.WORKSPACE}/docroot/sites/default/files/"
        sh "sudo chmod 770 ${env.WORKSPACE}/docroot/sites/default/*.inc"
    }
    // Enable rewrite in htaccess file.
    stage('htaccess'){
        sh returnStatus: true, script: "sudo sed -i \'s|# RewriteBase /|RewriteBase /|\' ${env.WORKSPACE}/docroot/.htaccess"
    }
    // Run the drush command for the website.
    stage('DrushStuff'){
        if (SERVER.contains('webserver6')){
            sh "drush9 -r ${env.WORKSPACE}/docroot cr -y"
            sh "drush9 -r ${env.WORKSPACE}/docroot cim -y"
            sh "drush9 -r ${env.WORKSPACE}/docroot updb -y"
            sh "drush9 -r ${env.WORKSPACE}/docroot cr -y"
        }
        else {
            sh "drush -r ${env.WORKSPACE}/docroot cr -y"
            sh "drush -r ${env.WORKSPACE}/docroot cim -y"
            sh "drush -r ${env.WORKSPACE}/docroot updb -y"
            sh "drush -r ${env.WORKSPACE}/docroot cr -y"
        }
    }
    }
    catch (caughtError) {
        err = caughtError
        currentBuild.result = "FAILURE"
    }
    finally {
        step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'DevelopersRecipientProvider']])])
    if (err) {
        throw err
    }
    }
    }
}
