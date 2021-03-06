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
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
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
                    schemaPropertiesFileName = 'schema.properties'
                }
                // set description of the build
                script {
                    currentBuild.description = "${params.GRADLE_TASKS}"
                }
                // check out Git repo and read a property value from a file
                dir(dbSchemaJobsDir) {
                    git url: "${gretlJobRepoUrl}", branch: "${params.BRANCH ?: 'main'}", changelog: false
                    script {
                        def schemaProperties = readProperties file: "topics/${params.TOPIC_NAME}/${params.SCHEMA_DIRECTORY_NAME}/${schemaPropertiesFileName}"
                        def databasesProperty = schemaProperties['databases'] ?: 'edit'
                        // must be an undeclared variable so it goes into the script binding:
                        databases = databasesProperty.split(',')
                    }
                }
                // check out privileges Git repo
                dir(dbSchemaPrivilegesDir) {
                    git url: "${privilegesRepoUrl}", branch: 'main', changelog: false, credentialsId: "${openshiftProjectName}-db-schema-privileges-deploy-key"
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
}
