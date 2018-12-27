#!/bin/sh
# Execute the event loop. This should be run in the background.
# Requires access to BERT_HOME
cd ${BERT_HOME}
MP=lib/bert-server.jar
MP=$MP:lib/bert-common.jar

java --module-path $MP -m bert.server/bert.server.main.Distributer etc/bert.xml