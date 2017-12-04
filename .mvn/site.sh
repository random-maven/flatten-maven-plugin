#!/bin/bash

#
# produce documentaion site
#

cd "${BASH_SOURCE%/*}/.."

./mvnw.sh plugin:report site -B
