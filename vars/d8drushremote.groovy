def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
        // Run the drush command for the website.
        stage('DrushStuff'){
            sh "ssh -tt ${config.SSH_SERVER} \"drush -r ${config.REMOTEDOCROOT}${config.WWW} cr -y\""        
            sh "ssh -tt ${config.SSH_SERVER} \"drush -r ${config.REMOTEDOCROOT}${config.WWW} cim -y\""
            sh "ssh -tt ${config.SSH_SERVER} \"drush -r ${config.REMOTEDOCROOT}${config.WWW} updb -y\""
            sh "ssh -tt ${config.SSH_SERVER} \"drush -r ${config.REMOTEDOCROOT}${config.WWW} cr -y\""        
        }
        //https://www.drupal.org/node/244924
        stage('Access'){
            sh "ssh -tt ${config.SSH_SERVER} \"sudo chown ${config.PROJECT_OWNER}:${config.PROJECT_GROUP} -R ${config.REMOTEDOCROOT} \""
            sh "ssh -tt ${config.SSH_SERVER} \"sudo find ${config.REMOTEDOCROOT}${config.WWW} -type d -exec chmod 750 {} \\;\""
            sh "ssh -tt ${config.SSH_SERVER} \"sudo find ${config.REMOTEDOCROOT}${config.WWW} -type f -exec chmod 650 {} \\;\""
            sh "ssh -tt ${config.SSH_SERVER} \"sudo find ${config.REMOTEDOCROOT}${config.WWW}/sites/default/files -type d -exec chmod 770 {} \\;\""
            sh "ssh -tt ${config.SSH_SERVER} \"sudo find ${config.REMOTEDOCROOT}${config.WWW}/sites/default/files -type f -exec chmod 660 {} \\;\""
        }      
    }
}
