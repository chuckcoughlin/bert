#!/bin/sh
# Execute the event loop. This should be run in the background.
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs

MP=lib/bluecove-2.1.0a.jar

# Allow debugging on port 8000
X="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"

echo "Run the Bluetooth Manager"
java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/bluecove.BluetoothManager