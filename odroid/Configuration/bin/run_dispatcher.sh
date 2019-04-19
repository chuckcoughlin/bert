#!/bin/sh
# Execute the event loop. This should be run in the background.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs

MP=mods/jssc-2.8.0.jar
MP=${MP}:mods/jackson-core-2.9.8.jar
MP=${MP}:mods/jackson-databind-2.9.8.jar
MP=${MP}:mods/jackson-annotations-2.9.8.jar
MP=${MP}:lib/bert-dispatcher.jar
MP=$MP:lib/bert-common.jar
MP=$MP:lib/bert-motors.jar

# Allow debugging on port 8000
X="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"

java $X --module-path $MP -m bert.server/bert.server.main.Dispatcher ${BERT_HOME}