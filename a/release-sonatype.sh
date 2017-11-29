#!/bin/bash

#
# publish artifact
#

base=$(git rev-parse --show-toplevel)

cd "$base"

mvn clean deploy -B -D skipTests -D invoker.skip=true -P attach-sources,distro-sonatype,sign-artifacts
