#!/bin/bash

cd "$(dirname "$0")"

JAVA_HOME=
#JAVA_HOME=/home/paris/jdks/zulu8.38.0.13-ca-fx-jdk8.0.212-linux_x64/bin

if [ -z "$JAVA_HOME" ]
then
    echo "Please set JAVA_HOME in script!"
    exit 1
fi

main_jar=$(find . -name "sqlite-browser*")
main_class=gr.paris.SqlBrowserApp

$JAVA_HOME/java -Xmx512m -Xms128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
-Dlog4j.configuration=file:./log4j.properties \
-cp lib/*:glib/*:$main_jar $main_class
