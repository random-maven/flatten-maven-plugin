#!/bin/bash

#
# invoke integration test
#

base=$(git rev-parse --show-toplevel)

cd "$base"

mvn clean install -B -D skipTests -D invoker.skip=true
