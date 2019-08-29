#!/bin/bash

cd "$(dirname "$0")"
export JAVA_HOME=/home/paris/jdks/zulu8.38.0.13-ca-fx-jdk8.0.212-linux_x64

rm -rf ../DIST
mkdir -p ../DIST
mkdir -p ../DIST/lib
mkdir -p ../DIST/glib

# build dependencies
mvn -U clean install -f ../DockFX-master/pom.xml
mvn -U clean install -f ../sqlfx/pom.xml
# build project
mvn -U clean install
mvn dependency:copy-dependencies

cp ./target/sqlite-browser-0.0.1-SNAPSHOT.jar ../DIST/
cp ./target/dependency/* ../DIST/lib
sqlfx=$(find ../DIST/lib -name "sqlfx*")
mv $sqlfx ../DIST/glib/
dockfx=$(find ../DIST/lib -name "moded-dockfx*")
mv $dockfx ../DIST/glib/
