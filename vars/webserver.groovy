def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
    
    // https://jenkins/view/MS/credentials/
    if ("${config.GIT_URL}".contains('gitlab')){ SSHAGENT = "" }
    if ("${config.GIT_URL}".contains('bitbucket')){ SSHAGENT = "" }

    // create the docroot folder for apache
    stage('CreateProjectFolder'){
        sh "sudo mkdir -p ${env.WORKSPACE}"
        sh "sudo mkdir -p ${env.WORKSPACE}/logs/"
        sh "sudo chown jenkins:${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
    }
    // because pantheon use web instead of docroot for folder name
    stage('WebDocroot'){
        if ("${config.PANTHEON}".contains('YES')){
            dir("${env.WORKSPACE}") {
            // checkout the git
            checkout([$class: 'GitSCM', branches: [[name: "${env.BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "$SSHAGENT", url: "${config.GIT_URL}"]]])
        }
            sh "ln -nsf ${env.WORKSPACE}/web ${env.WORKSPACE}/docroot"
        }
    }
    stage('Access'){
        sh "sudo mkdir -p ${env.WORKSPACE}/docroot/"
        sh "sudo mkdir -p ${env.WORKSPACE}/docroot/sites/default/files"
        sh "sudo chown jenkins:${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
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
            sh 'composer clear-cache'
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
        sh "sudo chown jenkins:${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
        sh returnStatus: true, script: 'sudo apachectl configtest && sudo apachectl restart'
     }
    // Set the folder permission on the path for this node
    stage('SetPermission'){
        sh "sudo chown jenkins:${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
        sh "sudo chmod -R 755 ${env.WORKSPACE}/docroot/sites/default/"
        sh "sudo chmod -R 774 ${env.WORKSPACE}/docroot/sites/default/files/"
    }
    // Enable rewrite in htaccess file.
    stage('htaccess'){
        sh returnStatus: true, script: "sudo sed -i \'s|# RewriteBase /|RewriteBase /|\' ${env.WORKSPACE}/docroot/.htaccess"
    }
    }
}
