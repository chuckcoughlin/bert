#!/bin/sh
# Execute a test of CRC calculations. The test is canned, compare results to
# the robotis website: http://emanual.robotis.com/docs/en/dxl/protocol2
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs
MP=mods/jssc.jar
MP=${MP}:mods/jackson-core-2.9.7.jar
MP=${MP}:mods/jackson-databind-2.9.7.jar
MP=${MP}:mods/jackson-annotations-2.9.7.jar
MP=${MP}:lib/bert-common.jar
MP=${MP}:lib/bert-motors.jar

java --module-path $MP -m bert.motor/bert.motor.dynamixel.DxlMessage