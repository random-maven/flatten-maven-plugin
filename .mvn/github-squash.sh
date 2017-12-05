#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=f47c9111f830c749c8af951cf4cd0d90c3979179

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
