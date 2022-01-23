#!/bin/bash

modules="java.base,java.datatransfer,java.desktop,jdk.unsupported,java.logging,java.sql"
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules $modules --output jre
