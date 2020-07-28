#!/bin/bash

for file in $(find . -name "*")
do
	if [ -f "$file" ]
	then
		echo "Fixing file : $file ..."
		sed -i "s/res\//\/icons\//g" $file
		echo "done"
	fi
done