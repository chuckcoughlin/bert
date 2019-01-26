#!/bin/sh
# Execute the event loop. This should be run in the background.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs

MP=mods/jssc.jar
MP=${MP}:mods/jackson-core-2.9.7.jar
MP=${MP}:mods/jackson-databind-2.9.7.jar
MP=${MP}:mods/jackson-annotations-2.9.7.jar
MP=${MP}:lib/bert-dispatcher.jar
MP=$MP:lib/bert-common.jar
MP=$MP:lib/bert-motors.jar

java --module-path $MP -m bert.dispatcher/bert.dispatcher.main.Dispatcher ${BERT_HOME}