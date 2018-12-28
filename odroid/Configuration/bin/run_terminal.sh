#!/bin/sh
# Execute terminal access to robot control.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs
MP=lib/bert-terminal.jar
MP=$MP:lib/bert-common.jar

java --module-path $MP -m bert.terminal/bert.term.main.Terminal ${BERT_HOME}
