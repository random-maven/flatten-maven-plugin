#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=5a1d68c41e66c24cbb8adc1c0e3576d659a67ec2

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
