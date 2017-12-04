#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=4e8409169386aea662450d4a77c0896e670968b3

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
