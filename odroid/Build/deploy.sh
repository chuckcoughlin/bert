#!/bin/bash
# Working directory is the project directory (Build).
# Synchronize the robot with the current build products.
# The 'rsync' command is an efficient way to guarantee
# that now more files are transferred than necessary.
export PATH=$PATH:/usr/local/bin
cd ../Archive
echo "Synchronizing modularized jar files ..."
rsync -r mods bert:/usr/local/robot

cd ../Configuration
echo "Synchronizing configuration files, database ..."
rsync -r etc bin bert:/usr/local/robot

cd ..
echo "Synchronizing compiled jar files ..."
rsync -r lib bert:/usr/local/robot

echo "Robot update is complete."
