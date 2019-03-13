#!/bin/sh
# Execute terminal access to robot control.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs
MP=mods/sqlite-jdbc-3.23.1.jar
MP=${MP}:mods/antlr-runtime-4.7.2.jar
MP=${MP}:mods/jackson-core-2.9.8.jar
MP=${MP}:mods/jackson-databind-2.9.8.jar
MP=${MP}:mods/jackson-annotations-2.9.8.jar
MP=$MP:lib/bert-terminal.jar
MP=$MP:lib/bert-common.jar
MP=$MP:lib/bert-database.jar
MP=$MP:lib/bert-speech.jar

# Allow debugging on port 8001
X="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8001"

java $X --module-path $MP -m bert.terminal/bert.term.main.Terminal ${BERT_HOME}
