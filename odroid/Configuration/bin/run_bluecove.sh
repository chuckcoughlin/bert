#!/bin/sh
# Execute the event loop. This should be run in the background.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs

MP=lib/btj-1.0.1.jar

# Allow debugging on port 8000
X="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"

java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m btj.jni/btj.Main