#!/bin/sh
# Execute the robot control code in "offline" mode
# Clear logs first
cd ${BERT_HOME}
bin/clear_logs.sh
cd distribution/bertApp
export APP_HOME=`pwd`
cd bin
./bertApp -o ${BERT_HOME}