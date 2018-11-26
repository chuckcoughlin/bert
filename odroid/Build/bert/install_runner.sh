#!/bin/bash
# Working directory is the project directory.
# Script can be executed either from the top level or 
# "bert" sub-directory. This script compiles the local
# Java code and copies the executable onto the robot.
# The robot must be restarted for the changes to take
# effect.
export PATH=$PATH:/usr/local/bin
if [ -d bert ]
then
	cd bert
fi
gradle -i clean classes jar install
