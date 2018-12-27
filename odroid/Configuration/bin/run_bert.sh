#!/bin/sh
# Execute the robot control code. This must be run as a background task.
# Requires access to BERT_HOME
cd ${BERT_HOME}
MP=lib/bert-cliey.jar
MP=$MP:lib/bert-common.jar

java --module-path $MP -m bert.client/bert.client.main.Bert etc/bert.xml