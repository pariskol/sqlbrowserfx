#!/bin/bash

cd "$(dirname "$0")"

rm -rf dist
mkdir -p dist/sqlbrowserfx/lib

mvn clean package
mvn dependency:copy-dependencies

cp target/*.jar dist/sqlbrowserfx/
cp target/dependency/* dist/sqlbrowserfx/lib
cp log4j.properties dist/sqlbrowserfx/
cp sqlbrowser.db dist/sqlbrowserfx/
cp starters/* dist/sqlbrowserfx/
touch dist/sqlbrowserfx/recent-dbs.txt

chmod +x dist/sqlbrowserfx/*.sh
cd dist

tar -czvf sql-browser-fx.tar.gz *


