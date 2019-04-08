#!/bin/sh
# Execute a read-write test of a serial port
cd ${BERT_HOME}
MP=mods/jssc-2.8.0.jar
MP=${MP}:lib/bert-common.jar
MP=${MP}:lib/bert-motors.jar

echo "BERT_HOME=`pwd`"
echo $MP
java --module-path $MP -m bert.motor/bert.motor.main.PortTest