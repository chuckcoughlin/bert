#!/bin/sh
# Execute a test of the URDF file located in $BERT_HOME/etc
cd ${BERT_HOME}
mkdir -p logs
MP=mods/hipparchus-core-1.5.jar
MP=${MP}:mods/jackson-core-2.9.8.jar
MP=${MP}:mods/jackson-databind-2.9.8.jar
MP=${MP}:mods/jackson-annotations-2.9.8.jar
MP=${MP}:lib/bert-dispatcher.jar
MP=${MP}:lib/bert-common.jar
MP=${MP}:lib/bert-control.jar

echo "BERT_HOME=`pwd`"
echo $MP
java --module-path $MP -m bert.control/bert.control.model.Chain ${BERT_HOME}