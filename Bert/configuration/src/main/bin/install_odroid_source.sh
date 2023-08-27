#!/bin/bash
# Working directory is the project directory ${BERT_HOME}.
# Synchronize the robot build area with sources on the development
# machine. Currently the only JNI source to build is "btj-1.0.1".
# Make sure there is a valid wi-fi connection.

export PATH=$PATH:/usr/local/bin

cd ../../odroid-src
echo "Synchronizing source files for odroid ..."
rsync -r * bert:/home/chuckc

echo "Robot source update is complete."
