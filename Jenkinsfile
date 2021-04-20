pipeline {
	agent none

	triggers {
		pollSCM 'H/10 * * * *'
		upstream(upstreamProjects: "spring-data-commons/2.2.x", threshold: hudson.model.Result.SUCCESS)
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage("Test") {
			when {
				anyOf {
					branch '3.2.x'
					not { triggeredBy 'UpstreamCause' }
				}
			}
			parallel {
				stage("test: baseline") {
					agent {
						docker {
							image 'adoptopenjdk/openjdk8:latest'
							label 'data'
							args '-u root -v /var/run/docker.sock:/var/run/docker.sock -v $HOME:/tmp/jenkins-home'
						}
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml clean dependency:list test -Dsort -U -B -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-elasticsearch'
					}
				}
			}
		}
		stage('Release to artifactory') {
			when {
				anyOf {
					branch '3.2.x'
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				docker {
					image 'adoptopenjdk/openjdk8:latest'
					label 'data'
					args '-u root -v /var/run/docker.sock:/var/run/docker.sock -v $HOME:/tmp/jenkins-home'
				}
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}

			steps {
				sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Pci,artifactory -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-elasticsearch ' +
						'-Dartifactory.server=https://repo.spring.io ' +
						"-Dartifactory.username=${ARTIFACTORY_USR} " +
						"-Dartifactory.password=${ARTIFACTORY_PSW} " +
						"-Dartifactory.staging-repository=libs-snapshot-local " +
						"-Dartifactory.build-name=spring-data-elasticsearch " +
						"-Dartifactory.build-number=${BUILD_NUMBER} " +
						'-Dmaven.test.skip=true clean deploy -U -B'
			}
		}
		stage('Publish documentation') {
			when {
				branch '3.2.x'
			}
			agent {
				docker {
					image 'adoptopenjdk/openjdk8:latest'
					label 'data'
					args '-u root -v /var/run/docker.sock:/var/run/docker.sock -v $HOME:/tmp/jenkins-home'
				}
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}

			steps {
				sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Pci,distribute -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-elasticsearch ' +
						'-Dartifactory.server=https://repo.spring.io ' +
						"-Dartifactory.username=${ARTIFACTORY_USR} " +
						"-Dartifactory.password=${ARTIFACTORY_PSW} " +
						"-Dartifactory.distribution-repository=temp-private-local " +
						'-Dmaven.test.skip=true clean deploy -U -B'
			}
		}
	}

	post {
		changed {
			script {
				slackSend(
						color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
						channel: '#spring-data-dev',
						message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
				emailext(
						subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
						mimeType: 'text/html',
						recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
						body: "<a href=\"${env.BUILD_URL}\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
			}
		}
	}
}
