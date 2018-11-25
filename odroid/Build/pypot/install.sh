#!/bin/bash
# Working directory is the project directory (Build).
# Script can be executed either from there or pypot sub-directory
# This is a bit superfluous as there is a pypot installer using apt-get.
export PATH=$PATH:/usr/local/bin
if [ `basename ${PWD}` = "pypot" ]
then
	cd ..
fi

cd ../PyPot

rsync -r ci pypot setup.py bert:~/pypot
echo "PyPot download complete."
