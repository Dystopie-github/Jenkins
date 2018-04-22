def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
        def sshServer = "${config.SSH_SERVER}"
        stage('freeSpace') {
            def freeSpace =  sh (script: """ssh -tt ${sshServer} \"df -h | grep dev | head -n 1 | awk '{ print \\\$5 }' | sed \"s/%//\" \" """, returnStdout: true).trim()
            int value = "$freeSpace".toInteger()
            if ( value > 90 ){
               currentBuild.result = 'FAILURE'
               return
            }
        }
    }
}