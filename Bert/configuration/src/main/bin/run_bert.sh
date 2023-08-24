#!/bin/sh
# Execute the robot control code on the build system
# Clear logs first
cd ${BERT_HOME}
mkdir -p logs
cd distribution/bertApp
export APP_HOME=`pwd`
cd bin
./clear_logs.sh
./bertApp ${BERT_HOME}