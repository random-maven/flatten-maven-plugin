#!/bin/bash

#
# perform local install
#

cd "${BASH_SOURCE%/*}/.."

./mvnw.sh clean install -B
