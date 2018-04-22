def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
        // Run the drush command for the website.
        stage('DrushStuff'){
            sh "drush -r ${env.WORKSPACE}/docroot cr -y"        
            sh "drush -r ${env.WORKSPACE}/docroot cim -y"
            sh "drush -r ${env.WORKSPACE}/docroot updb -y"
            sh "drush -r ${env.WORKSPACE}/docroot cr -y"        
        }
        //https://www.drupal.org/node/244924
        stage('Access'){
            sh "sudo chown jenkins:${config.PROJECT_GROUP} -R ${env.WORKSPACE}"
            sh "sudo find ${env.WORKSPACE}/docroot/ -type d -exec chmod 750 {} \\;"
            sh "sudo find ${env.WORKSPACE}/docroot/ -type f -exec chmod 650 {} \\;"
            sh "sudo find ${env.WORKSPACE}/docroot/sites/default/files -type d -exec chmod 770 {} \\;"
            sh "sudo find ${env.WORKSPACE}/docroot/sites/default/files -type f -exec chmod 660 {} \\;"
        }      
    }
}