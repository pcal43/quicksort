#!/bin/sh

#
# Publish to curseforge.
#

set -eu

if [ -n "$(git status --porcelain)" ]; then
  echo "Working directory not clean, cannot release"
  exit 1
fi

if [ -z "${CURSEFORGE_TOKEN:-}" ]; then
    echo "Set CURSEFORGE_TOKEN"
    exit 1
fi

RELEASE_BRANCH='maintenance/1.19.4'
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "${CURRENT_BRANCH}" != "${RELEASE_BRANCH}" ]; then
  echo "Releases must be performed on ${RELEASE_BRANCH}.  Currently on '${CURRENT_BRANCH}'"
  exit 1
fi

#
# Do curseforge release
#
./gradlew curseforge
