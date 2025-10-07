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

//   parameters {
//     booleanParam(name: 'dryRun', defaultValue: true, description: 'whether the build should push the changes or not')
//   }

  stages {
    stage('build') {
      steps {
        script {
          maven cmd: "-f build/lemminx/pom.xml clean install -DskipTests"
        }
      }
    }
  }
}