#!/bin/bash

cd "$(dirname "$0")"

export JAVA_HOME=/home/paris/jdks/zulu8.38.0.13-ca-fx-jdk8.0.212-linux_x64

mkdir -p ../DIST
mkdir -p ../DIST/lib
mkdir -p ../DIST/glib

cd ../DockFX-master
mvn clean package
mvn dependency:copy-dependencies

cp ./target/moded-dockfx-1.0.0-SNAPSHOT.jar ../DIST/glib
cp ./target/moded-dockfx-1.0.0-SNAPSHOT.jar ../sql-lite-bowser/lib
cp ./target/dependency/* ../DIST/lib	

cd ../sqlfx
mvn clean package
mvn dependency:copy-dependencies

cp ./target/sql-fx-1.0.0-SNAPSHOT.jar ../DIST/glib
cp ./target/dependency/* ../DIST/lib	
cp ./target/sql-fx-1.0.0-SNAPSHOT.jar ../sql-lite-bowser/lib

cd ../sql-lite-bowser
mvn clean package
mvn dependency:copy-dependencies

cp ./target/sqlite-browser-0.0.1-SNAPSHOT.jar ../DIST/
cp ./target/dependency/* ../DIST/lib	

#mkdir -p out
#mvn dependency:copy-dependencies
#mkdir -p out/lib
#cp -r ./target/dependency/* out/lib/
#mvn clean package
#cp ./target/*.jar out/

