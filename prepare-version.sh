#!/bin/bash

# fail fast and safely:
# -e  : exit immediately if a command fails
# -u  : treat unset variables as errors
# -o pipefail : catch errors in pipelines (fail if any command fails, not just the last one)
set -euo pipefail

cd "$(dirname "$0")"

version=$1

if [ -z "$version" ]; then
  echo "Provide target version!"
  exit 1
fi

# update pom.xml with release version
sed -i "0,/<version>.*<\/version>/s//<version>$version<\/version>/" pom.xml
git add pom.xml
git commit -m "prepare release $version"
git tag "$version"
git push --tags

# increase version (bump patch by default)
IFS='.' read -r major minor patch <<< "$version"
if [[ -z "$patch" ]]; then
  # If version has only major.minor, default patch=0
  patch=0
fi
newVersion="$major.$minor.$((patch+1))"

# update pom.xml with new snapshot version
sed -i "0,/<version>.*<\/version>/s//<version>$newVersion-SNAPSHOT<\/version>/" pom.xml
git add pom.xml
git commit -m "restore pom after release"
git push origin development

# merge into master
git checkout master
git merge "$version"
git push origin master

mvn clean install

# switch back to development
git checkout development
mvn clean install
