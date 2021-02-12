node('master') { // need a few lines of scripted pipeline before the declarative pipeline...
    stage('Prepare') {
        // store a couple of environment variable values that are available on master only
        gretlJobRepoUrl = env.GRETL_JOB_REPO_URL_DB_SCHEMA
        privilegesRepoUrl = env.GRETL_JOB_REPO_URL_DB_SCHEMA_PRIVILEGES
        openshiftProjectName = env.PROJECT_NAME
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
                // set variables
                script {
                    dbSchemaJobsDir = 'db-schema-jobs'
                    dbSchemaPrivilegesDir = 'db-schema-privileges'
                    dbSchemaJobsSharedSchemaDirPath = 'shared/schema'
                }
                // set description of the build
                script {
                    currentBuild.description = "${params.buildDescription}"
                }
                // check out Git repo and read a property value from a file
                dir(dbSchemaJobsDir) {
                    git url: "${gretlJobRepoUrl}", branch: "${params.BRANCH ?: 'main'}", changelog: false
                    script {
                        def createSchemaProperties = readProperties file: "topics/${params.TOPIC_NAME}/${params.SCHEMA_DIRECTORY_NAME}/createSchema.properties"
                        def databasesProperty = createSchemaProperties['databases'] ?: 'edit'
                        // must be an undeclared variable so it goes into the script binding:
                        databases = databasesProperty.split(',')
                    }
                }
                // check out privileges Git repo
                dir(dbSchemaPrivilegesDir) {
                    git url: "${privilegesRepoUrl}", branch: "${params.BRANCH ?: 'main'}", changelog: false, credentialsId: "${openshiftProjectName}-db-schema-privileges-deploy-key"
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
                dir("${dbSchemaJobsDir}/${dbSchemaJobsSharedSchemaDirPath}") {
                    sh "gretl -PtopicName=${params.TOPIC_NAME} -PschemaDirName=${params.SCHEMA_DIRECTORY_NAME} -PdbName=edit ${params.GRADLE_TASKS}"
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
                dir("${dbSchemaJobsDir}/${dbSchemaJobsSharedSchemaDirPath}") {
                    sh "gretl -PtopicName=${params.TOPIC_NAME} -PschemaDirName=${params.SCHEMA_DIRECTORY_NAME} -PdbName=pub ${params.GRADLE_TASKS}"
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
