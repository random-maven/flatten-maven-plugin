#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=b1a06d8209449c2c6821e452a3e90bf2d0c78539

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
