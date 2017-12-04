#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=a6b651210bfb36d9de314bf48e3bc25cef13a6e5

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
