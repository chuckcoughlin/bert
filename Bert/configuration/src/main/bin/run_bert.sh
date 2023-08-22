#!/bin/sh
# Execute the robot control code on the build system
cd ${BERT_HOME}
mkdir -p logs
cd distribution/bertApp
export APP_HOME=`pwd`
cd bin
./bertApp