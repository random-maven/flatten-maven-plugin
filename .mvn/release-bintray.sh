#!/bin/bash

#
# publish artifact
#

cd "${BASH_SOURCE%/*}/.."

./mvnw.sh clean deploy -B -D skipTests -D invoker.skip=true -P attach-sources,distro-bintray
