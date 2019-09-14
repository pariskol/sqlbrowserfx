#!/bin/bash

cd "$(dirname "$0")"

export JAVA_HOME=/home/paris/jdks/zulu8.38.0.13-ca-fx-jdk8.0.212-linux_x64

rm -rf dist
mkdir -p dist/lib

mvn clean package
mvn dependency:copy-dependencies

cp target/sqlite-browser-0.0.1-SNAPSHOT.jar dist/
cp target/dependency/* dist/lib
cp log4j.properties dist/
cp sqlbrowser.db dist/


