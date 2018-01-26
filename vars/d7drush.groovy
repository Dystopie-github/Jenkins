def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {

    // Run the drush command for the website.
    stage('DrushStuff'){
        sh "drush -r ${env.WORKSPACE}/docroot updb -y"
        sh "drush -r ${env.WORKSPACE}/docroot cc all -y"        
    }
    }
}
