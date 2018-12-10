#!/bin/bash
# Working directory is the project directory (Build).
# Copy configuration files to the robot.
export PATH=$PATH:/usr/local/bin

cd ../Configuration
rsync -r etc bin bert:/usr/local/robot
echo "Configuration download complete."
