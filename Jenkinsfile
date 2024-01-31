def p = [:]
node {
		checkout scm
		p = readProperties interpolate: true, file: 'ci/pipeline.properties'
}

pipeline {
	agent none

	triggers {
		pollSCM 'H/10 * * * *'
		upstream(upstreamProjects: "spring-data-commons/main", threshold: hudson.model.Result.SUCCESS)
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage("test: baseline (main)") {
			when {
				beforeAgent(true)
				anyOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 30, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials("${p['artifactory.credentials']}")
				DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
				DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
			}

			steps {
				script {
					docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.docker']) {
						sh "PROFILE=none JENKINS_USER_NAME=${p['jenkins.user.name']} ci/verify.sh"
						sh "JENKINS_USER_NAME=${p['jenkins.user.name']} ci/clean.sh"
					}
				}
			}
		}

		stage("Test other configurations") {
			when {
				beforeAgent(true)
				allOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			parallel {
				stage("test: baseline (next)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials("${p['artifactory.credentials']}")
						DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
						DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
					}
					steps {
						script {
							docker.image(p['docker.java.next.image']).inside(p['docker.java.inside.docker']) {
								sh "PROFILE=none JENKINS_USER_NAME=${p['jenkins.user.name']} ci/verify.sh"
								sh "JENKINS_USER_NAME=${p['jenkins.user.name']} ci/clean.sh"
							}
						}
					}
				}
			}
		}

		stage('Release to artifactory') {
			when {
				beforeAgent(true)
				anyOf {
					branch(pattern: "main|(\\d\\.\\d\\.x)", comparator: "REGEXP")
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 20, unit: 'MINUTES') }
			environment {
				ARTIFACTORY = credentials("${p['artifactory.credentials']}")
				DEVELOCITY_CACHE = credentials("${p['develocity.cache.credentials']}")
				DEVELOCITY_ACCESS_KEY = credentials("${p['develocity.access-key']}")
			}
			steps {
				script {
					docker.image(p['docker.java.main.image']).inside(p['docker.java.inside.basic']) {
							sh 'MAVEN_OPTS="-Duser.name=' + "${p['jenkins.user.name']}" + ' -Duser.home=/tmp/jenkins-home" ' +
								"DEVELOCITY_CACHE_USERNAME=${DEVELOCITY_CACHE_USR} " +
								"DEVELOCITY_CACHE_PASSWORD=${DEVELOCITY_CACHE_PSW} " +
								"GRADLE_ENTERPRISE_ACCESS_KEY=${DEVELOCITY_ACCESS_KEY} " +
								"./mvnw -s settings.xml -Pci,artifactory -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-elasticsearch-non-root " +
								"-Dartifactory.server=${p['artifactory.url']} " +
								"-Dartifactory.username=${ARTIFACTORY_USR} " +
								"-Dartifactory.password=${ARTIFACTORY_PSW} " +
								"-Dartifactory.staging-repository=${p['artifactory.repository.snapshot']} " +
								"-Dartifactory.build-name=spring-data-elasticsearch " +
								"-Dartifactory.build-number=spring-data-elasticsearch-${BRANCH_NAME}-build-${BUILD_NUMBER} " +
								"-Dmaven.test.skip=true clean deploy -U -B"
					}
				}
			}
		}
	}

	post {
		changed {
			script {
				emailext(
						subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
						mimeType: 'text/html',
						recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
						body: "<a href=\"${env.BUILD_URL}\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
			}
		}
	}
}
