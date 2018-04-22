def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
        def SSH_SERVER = "${config.SSH_SERVER}"
        def files = "$BUILD_NUMBER\".tar.gz\""
	    def path; if (SSH_SERVER.contains('acquia')){
	        path = sh (script: "ssh -tt ${config.SSH_SERVER} \"pwd\"", returnStdout: true).trim()
	    }else{
	        path = "/tmp"
	    }
        if ("${config.DOWNTOOL}".contains('drush')){
            stage ('DrushBackup'){
                sh "ssh -tt ${config.SSH_SERVER} \"cd ${config.REMOTE_DOCROOT} && drush archive-dump ${config.MULTISITES} --destination=${path}/${files}\""
            }
        }else{
            stage ('MysqldumpBackup'){
               sh "ssh -tt ${config.SSH_SERVER} \"mysqldump -h ${config.dbhost} -u ${config.dbuser} -p${config.dbpass} ${config.db} > ${path}/${files}\""
            }
        }
        stage ('scpdata'){
                sh "scp -r ${config.SSH_SERVER}:${path}/${files} /home/jenkins/workspace/website/"
		}
        stage ('removeRemoteFile'){
		    sh "ssh -tt ${config.SSH_SERVER} \"rm -rf ${path}/${files}\""
        }
        stage ('scpdataToWebServer'){
                sh "scp -r /home/jenkins/workspace/website/${files} ${config.SSH_SERVER}:/home/jenkins/workspace/website"
		}
        stage ('removeLocalFile'){
		    sh "rm -rf /home/jenkins/workspace/website/${files}"
        }
	}
}