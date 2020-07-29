#!/bin/bash

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
cp sqlbrowser.db dist/sqlbrowserfx/
#cp starters/* dist/sqlbrowserfx/
cp sqlbrowserfx.properties dist/sqlbrowserfx/
touch dist/sqlbrowserfx/recent-dbs.txt

chmod +x dist/sqlbrowserfx/*.sh
cd dist

latest_tag=$(git describe)
tar -czvf sqlbrowserfx-$latest_tag.tar.gz *
