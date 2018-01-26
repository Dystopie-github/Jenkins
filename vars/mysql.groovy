def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node("${config.SERVER}") {
    
    //use this block to create the .inc file according to the branch "type"
    def BRANCH_NAME = "${env.BRANCH}"
    def BRANCH_INC = "dev"
    if (['dev', 'msd', 'ms-dev', 'msdev'].any { it == BRANCH_NAME }){ BRANCH_INC = "dev" }
    if (['stage', 'mss', 'ms-stage', 'ms-stg', 'msstage'].any { it == BRANCH_NAME }){ BRANCH_INC = "stage" }
    if (['master'].any { it == BRANCH_NAME }){ BRANCH_INC = "prod" }
    
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
    stage('DatabaseAccess'){
        sh "sudo chmod 770 ${env.WORKSPACE}/docroot/sites/default/*.inc"   
    }
    }
}
