#!/bin/bash -x

set -euo pipefail

export JENKINS_USER=${JENKINS_USER_NAME}

MAVEN_OPTS="-Duser.name=${JENKINS_USER} -Duser.home=/tmp/jenkins-home" \
  ./mvnw -s settings.xml clean -Dscan=false -Dmaven.repo.local=/tmp/jenkins-home/.m2/spring-data-elasticsearch -Ddevelocity.storage.directory=/tmp/jenkins-home/.develocity-root
