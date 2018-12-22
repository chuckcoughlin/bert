#!/bin/bash
# Working directory is the project directory (Build).
# Copy configuration files to the robot and to the local test area.
export PATH=$PATH:/usr/local/bin
cd ../Configuration
cp -a etc bin ${BERT_HOME}
rsync -r etc bin bert:/usr/local/robot
echo "Configuration download complete."
