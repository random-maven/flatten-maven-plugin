#!/bin/bash

#
# invoke integration test
#

cd "${BASH_SOURCE%/*}/.."

./mvnw.sh clean verify -B
