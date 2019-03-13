#!/bin/sh
# Execute the robot control code. This must be run as a background task.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs
MP=mods/sqlite-jdbc-3.23.1.jar
MP=${MP}:mods/antlr-runtime-4.7.2.jar
MP=${MP}:mods/jackson-core-2.9.8.jar
MP=${MP}:mods/jackson-databind-2.9.8.jar
MP=${MP}:mods/jackson-annotations-2.9.8.jar
MP=$MP:lib/bert-command.jar
MP=$MP:lib/bert-common.jar
MP=$MP:lib/bert-database.jar
MP=$MP:lib/bert-speech.jar

# Allow debugging on port 8002
X="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8002"

java $X --module-path $MP -m bert.command/bert.command.main.Command ${BERT_HOME}