#!/bin/sh
# Execute the robot control code. This must be run as a background task.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs
MP=lib/bert-client.jar
MP=$MP:lib/bert-common.jar

java --module-path $MP -m bert.client/bert.client.main.Bert ${BERT_HOME}