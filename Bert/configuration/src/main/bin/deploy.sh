#!/bin/bash
# Working directory is the project directory ${BERT_HOME}.
# Synchronize the robot with the current build products.
# The 'rsync' command is an efficient way to guarantee
# that no more files are transferred than necessary.
# Make sure there is a valid wi-fi connection.

export PATH=$PATH:/usr/local/bin
cd ../Archive
echo "Synchronizing modularized jar files ..."
rsync -r mods bert:/usr/local/robot

cd ../Configuration
echo "Synchronizing configuration files, database ..."
rsync -r etc bin pylib bert:/usr/local/robot

cd ../Build
echo "Synchronizing application jar files ..."
rsync -r lib bert:/usr/local/robot

echo "Robot update is complete."
