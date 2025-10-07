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
          maven cmd: "clean ${phase} -DskipTests"
        }
        archiveArtifacts 'org.eclipse.lemminx/target/*.jar'
        withChecks('Maven Issues') {
          recordIssues tools: [mavenConsole()], qualityGates: [[threshold: 1, type: 'TOTAL']]
        }
      }
    }
  }
}

def isReleaseOrMasterBranch() {
  return env.BRANCH_NAME == 'main' || env.BRANCH_NAME.startsWith('release/') 
}