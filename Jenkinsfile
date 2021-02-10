node('master') { // need a few lines of scripted pipeline before the declarative pipeline...
    stage('Prepare') {
        //gretlJobRepoUrl = env.GRETL_JOB_REPO_URL_DB_SCHEMA
        gretlJobRepoUrl = 'https://github.com/schmandr/db-schema-jobs.git'
    }
}

pipeline {
    agent none
    options {
        buildDiscarder(logRotator(numToKeepStr: '25'))
        disableConcurrentBuilds()
        timeout(time: 7, unit: 'DAYS')
    }
    stages {
        stage('DB schema') {
            agent { label 'gretl-ili2pg4' }
//             input {
//                 message "Just to be sure... Do you really want to run the potentially dangerous tasks \'${params.GRADLE_TASKS}\'?"
//                 ok "OK"
//             }
            steps {
                script { currentBuild.description = "${params.buildDescription}" }
                git url: "${gretlJobRepoUrl}", branch: "${params.BRANCH ?: 'main'}", changelog: false
                // TODO: Store in a variable
                dir('shared/schema') {
                    sh "gretl -PtopicName=${params.TOPIC_NAME} -PschemaDirName=${params.SCHEMA_DIRECTORY_NAME} -PdbName=${params.TARGET_DB} ${params.GRADLE_TASKS}"
                    //sh 'gretl grantPermissions'
                }
            }
        }
    }
    post {
        // TODO: Possibly unnecessary
        unsuccessful {
            emailext (
                to: '${DEFAULT_RECIPIENTS}',
                recipientProviders: [requestor()],
                subject: "GRETL-Job ${JOB_NAME} (${BUILD_DISPLAY_NAME}) ist fehlgeschlagen",
                body: "Die Ausführung des GRETL-Jobs ${JOB_NAME} (${BUILD_DISPLAY_NAME}) war nicht erfolgreich. Details dazu finden Sie in den Log-Meldungen unter ${RUN_DISPLAY_URL}."
            )
        }
    }
}
