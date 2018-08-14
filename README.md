# Jenkins Habitat Executor Plugin

This plugin allows for the abstraction of building, uploading, and promoting
habitat packages.

## Pre-Reqs

- Must have Jenkins setup for building habitat packages  
  - A designated origin and the corresponding keys uploaded to slaves to run job  
- Habitat Auth Token specified as a Jenkins credential  

## Usage

### Example Build and Upload Job

```
pipeline {
    agent any

    environment {
        HAB_NOCOLORING = true
        HAB_BLDR_URL = 'https://bldr.habitat.sh/'
        HAB_ORIGIN = 'skylerto'
    }

    stages {
        stage('scm') {
            steps {
                git url: 'https://github.com/skylerto/nexus-habitat.git', branch: 'master'
            }
        }
        stage('build') {
            steps {
                habitat task: 'build', directory: '.', origin: "${env.HAB_ORIGIN}"
            }
        }
        stage('upload') {
            steps {
                withCredentials([string(credentialsId: 'depot-token', variable: 'HAB_AUTH_TOKEN')]) {
                    habitat task: 'upload', authToken: env.HAB_AUTH_TOKEN, lastBuildFile: "${workspace}/results/last_build.env", bldrUrl: "${env.HAB_BLDR_URL}"
                }
            }
        }
    }
}
```

### Example Promotion/Release Job

```
pipeline {
    agent any

    environment {
        HAB_NOCOLORING = true
        HAB_BLDR_URL = 'https://bldr.habitat.sh/'
    }

    stages {
        stage('promote') {
            steps {
              script {
                env.HAB_CHANNEL = input message: 'Please provide a channel',
                    parameters: [
                      string(description: 'Habitat Channel', name: 'channel')
                ]
              }
              script {
                env.HAB_PKG = input message: 'Please provide an artifact',
                    parameters: [
                      string(description: 'Habitat Package', name: 'package')
                ]
              }
              //Hard-coding your "${env.HAB_CHANNEL}" removes the need to ask for it (improved automation)
              //You could also set env.HAB_PKG programatically, like so:
              /*
              script {
                env.HAB_PKG = sh ( 
                     script: "ls -t "{$workspace}"/results | grep hart | head -n 1",
                     returnStdout: true
                     ).trim()
              }
              */
              withCredentials([string(credentialsId: 'depot-token', variable: 'HAB_AUTH_TOKEN')]) {
                  habitat task: 'promote', channel: "${env.HAB_CHANNEL}", authToken: "${env.HAB_AUTH_TOKEN}", artifact: "${env.HAB_PKG}", bldrUrl: "${env.HAB_BLDR_URL}"
              }
            }
        }
    }
}
```

### Other Shiny New Features

```
pipeline {
    agent any

    environment {
        HAB_NOCOLORING = true
        HAB_BLDR_URL = 'https://bldr.habitat.sh/'
        HAB_AUTH_TOKEN = 'my-hab-auth-token-name'
        HAB_PKG = 'core/googlemock/1.8.0/20180609191841'
    }

    stages {
        stage('instantiate_variable'){
            steps {
                script {
                    env.PKG_FILE_NAME = sh(
                    script:  "ls -t ${workspace}/results/ | grep hart | head -n 1",
                    returnStdout: true
                    ).trim()
                   }

                script {
                    env.TMP_PKG_NAME = sh(
                    script: "echo ${env.PKG_FILE_NAME} | sed 's/-x86_64-linux.hart//' | cut -d '-' -f 2-20 | sed 's/\\(.*\\)-/\\1\\//' | sed 's/\\(.*\\)-/\\1\\//'",
                    returnStdout: true
                    ).trim()
                }

                script {
                    env.DOCKER_IMG_NAME = sh(
                    script: "echo ${env.TMP_PKG_NAME} | cut -d '/' -f 1",
                    returnStdout: true
                    ).trim()
                    }

                script {
                    env.PKG_NAME = "${env.HAB_ORIGIN}" + '/' + "${env.TMP_PKG_NAME}"
                    }
              }
        }

        stage('demote') {
              //demotes habitat pkg from specified channel in builder
              habitat task: 'demote', channel: 'development', authToken: "${env.HAB_AUTH_TOKEN}", artifact: "${env.HAB_PKG}", bldrUrl: "${env.HAB_BLDR_URL}"
            }
        stage('channels') {
              //lists channels specified habitat pkg belongs to
              habitat task: 'channels', authToken: "${env.HAB_AUTH_TOKEN}", artifact: "${env.HAB_PKG}", bldrUrl: "${env.HAB_BLDR_URL}"
            }
        stage('config') {
              //lists configuration options build into package
              habitat task: 'config', artifact: "${env.PKG_NAME}", authToken: "${env.HAB_AUTH_TOKEN}", bldrUrl: "${env.HAB_BLDR_URL}"
            }
        stage('export') {
              //export hart file to any format ("aci", "cf", "docker", "kubernetes", "mesos", "tar")
              //must provide a hart file instead of full pkg origin/name/path
              habitat task: 'export', format: "docker", lastBuildFile: "${workspace}/results/last_build.env", bldrUrl: "${env.HAB_BLDR_URL}"
            }
        stage('search') {
              //search an origin for all packages matching search string
              habitat task: 'search', searchString: '<my-search-string>', authToken: "${env.HAB_AUTH_TOKEN}", bldrUrl: "${env.HAB_BLDR_URL}"
            }
        stage('exec') {
              //execute commands within context of habitat pkg
              habitat task: 'exec', artifact: "${env.PKG_NAME}", command: '<your-command>'
            }
        stage('binlink') {
              //link habitat pkg to local system's bin executeable
              habitat task: 'binlink', path: '/usr/local/bin', binary 'bash', artifact: "${env.PKG_NAME}", authToken: "${env.HAB_AUTH_TOKEN}", bldrUrl: "${env.HAB_BLDR_URL}"
            }
        }
    }
}
```

The directory for the `build` task must be local to the repository as the studio is created under the root of the repository.
