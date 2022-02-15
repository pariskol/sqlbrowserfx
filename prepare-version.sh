#!/bin/bash

cd "$(dirname "$0")"

version=$1

if [ -z "$version" ]
then
  echo "Provide target version!"
  exit 1
fi

sed '0,/<version>.*<\/version>/s//<version>$version<\/version>/' pom.xml
git add pom.xml
git commit -m "prepare realase $version"
git tag $version
git push --tags

newVersion=$(groovy increaseVersion.groovy $version)
sed "0,/<version>.*<\/version>/s//<version>$newVersion-SNAPSHOT<\/version>/" pom.xml
git add pom.xml
git commit -m "restore pom after release"
git push origin development



