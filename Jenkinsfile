pipeline {
  agent {
    dockerfile {
      filename 'Dockerfile'
    }
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '60', artifactNumToKeepStr: '2'))
  }

  triggers {
    cron '@midnight'
  }

  stages {
    stage('build') {
      steps {
        script {
          def phase = isReleaseOrMasterBranch() ? 'deploy' : 'verify'
          maven cmd: "clean ${phase} -P ci"
          if (isReleaseOrMasterBranch()) {
            maven cmd: "org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom -DincludeLicenseText=true -DoutputFormat=json"
            uploadBOM(projectName: 'lemminx', projectVersion: 'master', bomFile: 'target/bom.json')
          }
        }
        archiveArtifacts 'org.eclipse.lemminx/target/*.jar'
        withChecks('Maven Issues') {
          recordIssues skipPublishingChecks: true, 
          tools: [mavenConsole()], 
          qualityGates: [[threshold: 1, type: 'TOTAL']], 
          filters: [excludeMessage('.*Skipped.*'), excludeMessage('.*Unknown keyword (meta:enum|deprecated).*')]
        }
        junit 'org.eclipse.lemminx/target/surefire-reports/**/*.xml' 
      }
    }
  }
}

def isReleaseOrMasterBranch() {
  return env.BRANCH_NAME == 'master' || env.BRANCH_NAME.startsWith('release/') 
}