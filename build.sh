#!/bin/bash
export JAVA_HOME=/home/paris/jdks/zulu8.38.0.13-ca-fx-jdk8.0.212-linux_x64

mkdir -p out
mvn dependency:copy-dependencies
mkdir -p out/lib
cp -r ./target/dependency/* out/lib/
mvn clean package
cp ./target/*.jar out/

