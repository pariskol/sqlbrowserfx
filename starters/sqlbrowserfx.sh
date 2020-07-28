#!/bin/bash

cd "$(dirname "$0")"

main_jar=$(find . -name "sqlbrowser*.jar")
main_class=gr.sqlbrowserfx.SqlBrowserFXApp

$JAVA_HOME/java -Xmx512m -Xms128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \
-Dlog4j.configuration=file:./log4j.properties \
-cp lib/*:glib/*:$main_jar $main_class $1