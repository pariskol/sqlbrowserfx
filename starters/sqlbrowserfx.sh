#!/bin/bash

cd "$(dirname "$0")"

main_jar=$(find . -name "sqlbrowser*.jar")
main_class=gr.sqlbrowserfx.GUIStarter

[ ! -z "$JAVA_HOME" ] && JAVA_HOME=$JAVA_HOME/bin/
${JAVA_HOME}java -Xmx512m -Xms128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100  -Dprism.lcdtext=false \
-cp lib/*:$main_jar $main_class
