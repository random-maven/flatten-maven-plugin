#!/bin/bash

#
# invoke integration test
#

base=$(git rev-parse --show-toplevel)

cd "$base"

mvn clean verify
