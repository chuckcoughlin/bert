#!/bin/sh
# Execute the robot control code on the build system
# Clear logs first
cd ${BERT_HOME}
bin/clear_logs.sh
cd distribution/bertApp
export APP_HOME=`pwd`
cd bin
./bertApp ${BERT_HOME}