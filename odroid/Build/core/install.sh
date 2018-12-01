#!/bin/bash
# Working directory is the project directory.
# Make current directory the project proxy area.
# Script can be executed either from the top level or proxy directory
export PATH=$PATH:/usr/local/bin
if [ -d core ]
then
	cd core
fi
gradle -i --scan clean assemble install
