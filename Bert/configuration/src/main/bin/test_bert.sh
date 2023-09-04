#!/bin/sh
# Execute the robot control code in "offline" mode on the test system
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home/
# Clear logs first
cd ${BERT_HOME}
bin/clear_logs.sh
cd distribution/bertApp
export APP_HOME=`pwd`
cd bin
./bertApp -o ${BERT_HOME}