#!/bin/bash
# Working directory is the project directory.
# Script can be executed either from the top level or 
# "bert" sub-directory. This script simply compiles
# the Java code locally and does not install on the robot.
export PATH=$PATH:/usr/local/bin
if [ -d bindings ]
then
	cd bindings
fi
if [ -d bert ]
then
	cd bert
fi
gradle -i classes jar --exclude-task processResources
