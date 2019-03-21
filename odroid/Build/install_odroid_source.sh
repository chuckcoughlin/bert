#!/bin/bash
# Working directory is the project directory (Build).
# Synchronize the robot build area with sources on the development
# machine. Currently the only JNI source to build is "bluez-jni".
# Make sure there is a valid wi-fi connection.

export PATH=$PATH:/usr/local/bin

cd ../../odroid-src
echo "Synchronizing source files for odroid ..."
rsync -r * bert:/home/chuckc

echo "Robot source update is complete."
