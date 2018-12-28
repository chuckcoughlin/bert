#!/bin/sh
# Execute the event loop. This should be run in the background.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs

MP=lib/bert-server.jar
MP=$MP:lib/bert-common.jar

java --module-path $MP -m bert.server/bert.server.main.Distributer ${BERT_HOME}