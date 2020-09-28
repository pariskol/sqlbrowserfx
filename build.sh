#!/bin/bash

get_version_from_pom() {
	grep -ioh -m 1 "<version>.*</version>" pom.xml | sed "s/<version>//g" | sed "s/<\/version>//g"
}

cd "$(dirname "$0")"

rm -rf dist
mkdir -p dist/sqlbrowserfx/lib

mvn clean package
if [ $? -ne 0 ]
then
  echo "Error : could not package project!"
  exit 1
fi
mvn dependency:copy-dependencies
if [ $? -ne 0 ]
then
  echo "Error : could not download dependencies!"
  exit 2
fi


cp target/sqlbrowserfx*.jar dist/sqlbrowserfx/sqlbrowserfx.jar
cp target/dependency/* dist/sqlbrowserfx/lib
cp log4j.properties dist/sqlbrowserfx/
cp sqlbrowser-for-build.db dist/sqlbrowserfx/sqlbrowser.db
cp starters/* dist/sqlbrowserfx/
cp sqlbrowserfx.properties dist/sqlbrowserfx/

version=$(get_version_from_pom)
chmod +x dist/sqlbrowserfx/*.sh
cd dist

tar -czvf sqlbrowserfx-$version.tar.gz *
