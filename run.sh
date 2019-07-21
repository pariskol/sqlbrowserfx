#!/bin/bash

cd "$(dirname "$0")"

main_jar=$(find . -name "sqlite-browser*")
main_class=gr.paris.SqlBrowserApp

java -Xms128m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
-Dlog4j.configuration=file:./log4j.properties \
-cp lib/*:glib/*:$main_jar $main_class
