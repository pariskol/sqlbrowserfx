#!/bin/bash

cat logo.txt
mkdir -p ~/sqlbrowserfx
cp -r * ~/sqlbrowserfx/
echo "Checking for .bash_aliases..."
[ ! -f .bash_aliases ] && touch ~/.bash_aliases
echo "Adding alias for SqlBrowserFX..."
[ $(grep -c "sqlfx" ~/.bash_aliases) -eq 0 ] && echo "alias sqlfx=~/sqlbrowserfx/sqlbrowserfx.sh" >> ~/.bash_aliases
echo "Installation finished"
