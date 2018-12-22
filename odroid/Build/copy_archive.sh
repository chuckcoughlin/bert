#!/bin/bash
# Working directory is the project directory (Build).
# Copy open-source jars to the robot. These are all
# Java modules.
export PATH=$PATH:/usr/local/bin
env
cd ../Archive
#rsync -r mods bert:/usr/local/robot
echo "Archive download complete."
