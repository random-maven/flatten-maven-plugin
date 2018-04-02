#!/bin/bash
set -e -u

#
# invoke integration test
#

cd "${BASH_SOURCE%/*}/.."

export JAVA_HOME=/usr/lib/jvm/java-8-jdk

./mvnw.sh clean verify -B -V
