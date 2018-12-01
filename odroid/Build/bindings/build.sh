#!/bin/bash
# Build the java bindings that control the robot.
# Script can be executed either from the top level or 
# "bindings" sub-directory. This script simply compiles
# the Java code locally and does not install on the robot.
# When run from eclipse, the working directory is "Build"
export PATH=$PATH:/usr/local/bin
if [ -d bindings ]
then
	cd bindings
fi
gradle clean classes jar install --exclude-task processResources
