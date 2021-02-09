println "BUILD_NUMBER = ${BUILD_NUMBER}"

// set default values
def gretlJobFilePath = 'topics/*/*'
def gretlJobFileName = 'createSchema.properties'
def jenkinsfileName = 'Jenkinsfile'
def jobPropertiesFileName = 'job.properties'

def jobNamePrefix = 'db_schema_'

// override default values if environment variables are set
// (Disable overriding for now):
// if ("${GRETL_JOB_FILE_PATH}") {
//   gretlJobFilePath = "${GRETL_JOB_FILE_PATH}"
//   println 'gretlJobFilePath set to ' + gretlJobFilePath
// }
// if ("${GRETL_JOB_FILE_NAME}") {
//   gretlJobFileName = "${GRETL_JOB_FILE_NAME}"
//   println 'gretlJobFileName set to ' + gretlJobFileName
// }


def baseDir = SEED_JOB.getWorkspace().toString()
println 'base dir: ' + baseDir

// search for files (gretlJobFileName) that are causing the creation of a GRETL-Job
def jobFilePattern = "${gretlJobFilePath}/${gretlJobFileName}"
println 'job file pattern: ' + jobFilePattern

def jobFiles = new FileNameFinder().getFileNames(baseDir, jobFilePattern)


// generate the jobs
println 'generating the jobs...'
for (jobFile in jobFiles) {

  // get the topic name (is at position 3 from the end of the jobFile path)
  def topicName = jobFile.split('/').getAt(-3)
  def schemaDirName = jobFile.split('/').getAt(-2)
  
  def jobName = "${jobNamePrefix}${topicName}${schemaDirName.minus('schema')}"
  println 'Job ' + jobName
//  println 'script file: ' + relativeScriptPath

  def pipelineFilePath = "${baseDir}/${jenkinsfileName}"

  // check if job provides its own Jenkinsfile
  def customPipelineFilePath = "${topicName}/${schemaDirName}/${jenkinsfileName}"
  if (new File(baseDir, customPipelineFilePath).exists()) {
    pipelineFilePath = customPipelineFilePath
    println 'custom pipeline file found: ' + customPipelineFilePath
  }
  def pipelineScript = readFileFromWorkspace(pipelineFilePath)

  // set defaults for job properties
  def properties = new Properties([
    'authorization.permissions':'nobody',
    'logRotator.numToKeep':'15',
    'parameters.fileParam':'none',
    'parameters.stringParam':'none',
    'triggers.upstream':'none',
    'triggers.cron':''
  ])
  def propertiesFilePath = "${topicName}/${schemaDirName}/${jobPropertiesFileName}"
  def propertiesFile = new File(baseDir, propertiesFilePath)
  if (propertiesFile.exists()) {
    println 'properties file found: ' + propertiesFilePath
    properties.load(new FileReader(propertiesFile))
  }
  
  def productionEnv = ("${OPENSHIFT_BUILD_NAMESPACE}" == 'agi-gretl-production')

  pipelineJob(jobName) {
    // if possible find different solution than setting as parameters:
    parameters {
      stringParam('TOPIC_NAME', "${topicName}", 'Name of the topic')
    }
    parameters {
      stringParam('SCHEMA_DIRECTORY_NAME', "${schemaDirName}", 'Name of the folder containing the schema specific definitions')
    }

    // the usual parameters below here:
    if (!productionEnv) { // we don't want the BRANCH parameter in production environment
      parameters {
        stringParam('BRANCH', 'main', 'Name of branch to check out')
      }
    }
    if (properties.getProperty('parameters.fileParam') != 'none') {
      parameters {
        fileParam(properties.getProperty('parameters.fileParam'), 'Select file to upload')
      }
    }
    if (properties.getProperty('parameters.stringParam') != 'none') {
      def propertyValues = properties.getProperty('parameters.stringParam').split(';')
      if (propertyValues.length == 3) {
        parameters {
          stringParam(propertyValues[0], propertyValues[1], propertyValues[2])
        }
      }
    }
    authorization {
      permissions(properties.getProperty('authorization.permissions'), ['hudson.model.Item.Build', 'hudson.model.Item.Read'])
    }
    if (properties.getProperty('logRotator.numToKeep') != 'unlimited') {
      logRotator {
        numToKeep(properties.getProperty('logRotator.numToKeep') as Integer)
      }
    }
    if (properties.getProperty('triggers.upstream') != 'none') {
      triggers {
        upstream(properties.getProperty('triggers.upstream'), 'SUCCESS')
      }
    }
    if (productionEnv) { // we want triggers only in production environment
      triggers {
        cron(properties.getProperty('triggers.cron'))
      }
    }
    if (properties.getProperty('nodeLabel') != null) {
      parameters {
        choiceParam('nodeLabel', [properties.getProperty('nodeLabel')], 'Label of the node that must run the job')
      }
    }
    definition {
      cps {
        script(pipelineScript)
        sandbox()
      }
    }
  }
}
