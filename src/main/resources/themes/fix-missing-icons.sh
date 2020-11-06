#!/bin/bash

source_theme="./flat-dark/icons"
target_theme="./flat-blue/icons"

for file in $(find $source_theme -type f -name "*")
do
if [ $(find $target_theme -type f -name "*" | grep -ic $(basename $file)) -eq 0 ]
then
    echo "Copying $(basename $file) ..."
    cp $file $target_theme/
fi
done
