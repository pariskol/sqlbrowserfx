#!/bin/bash

cat logo.txt
./build.sh
mkdir -p ~/sqlbrowserfx
cp -r dist/sqlbrowserfx/* ~/sqlbrowserfx/
cp starters/sqlbrowserfx.sh ~/sqlbrowserfx/
echo
echo "Fixing starter script.."
sed -i "s|#JAVA_HOME|export JAVA_HOME=$JAVA_HOME/bin|g" ~/sqlbrowserfx/sqlbrowserfx.sh
chmod +x ~/sqlbrowserfx/sqlbrowserfx.sh
echo "Checking for .bash_aliases..."
[ ! -f .bash_aliases ] && touch ~/.bash_aliases
echo "Adding alias for SqlBrowserFX..."
[ $(grep -c "sqlfx" ~/.bash_aliases) -eq 0 ] && echo "alias sqlfx=~/sqlbrowserfx/sqlbrowserfx.sh" >> ~/.bash_aliases
source ~/.bashrc
echo
echo "Installation finished"
echo
echo "Start sqlbrowserdfx from cli by running sqlfx"
