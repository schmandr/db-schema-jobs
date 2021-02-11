node('master') { // need a few lines of scripted pipeline before the declarative pipeline...
    stage('Prepare') {
        //gretlJobRepoUrl = env.GRETL_JOB_REPO_URL_DB_SCHEMA
        gretlJobRepoUrl = 'https://github.com/schmandr/db-schema-jobs.git'
    }
}

pipeline {
    agent {
        label 'gretl-ili2pg4'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '25'))
        disableConcurrentBuilds()
        timeout(time: 7, unit: 'DAYS')
    }
    stages {
        stage('Initialization') {
//            input {
//                message "Just to be sure... Do you really want to run the potentially dangerous tasks \'${params.GRADLE_TASKS}\'?"
//                ok "OK"
//            }
            steps {
                script { currentBuild.description = "${params.buildDescription}" }
                git url: "${gretlJobRepoUrl}", branch: "${params.BRANCH ?: 'main'}", changelog: false
                script {
                    def createSchemaProperties = readProperties file: "topics/${params.TOPIC_NAME}/${params.SCHEMA_DIRECTORY_NAME}/createSchema.properties"
                    def databasesProperty = createSchemaProperties['databases'] ?: 'edit'
                    // must be a undeclared variable so it goes into the script binding:
                    databases = databasesProperty.split(',')
                }
            }
        }
        stage('Edit DB') {
            when {
                expression {
                    databases.contains('edit')
                }
            }
            steps {
                // TODO: Store in a variable
                dir('shared/schema') {
                    sh "gretl -PtopicName=${params.TOPIC_NAME} -PschemaDirName=${params.SCHEMA_DIRECTORY_NAME} -PdbName=edit ${params.GRADLE_TASKS}"
                    //sh 'gretl grantPermissions'
                }
            }
        }
        stage('Publication DB') {
            when {
                expression {
                    databases.contains('pub')
                }
            }
            steps {
                // TODO: Store in a variable
                dir('shared/schema') {
                    sh "gretl -PtopicName=${params.TOPIC_NAME} -PschemaDirName=${params.SCHEMA_DIRECTORY_NAME} -PdbName=pub ${params.GRADLE_TASKS}"
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
