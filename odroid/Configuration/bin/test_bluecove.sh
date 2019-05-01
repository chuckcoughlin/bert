#!/bin/sh
# Execute canned bluecove tests
# Run one of 4 tests based on command arguments
cd ${BERT_HOME}
mkdir -p logs

MP=lib/bluecove-2.1.0a.jar

# Allow debugging on port 8000
X="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"

if [ $# -lt 1 ]
then
	echo "Usage: $0 arg"
	echo "   1 - simple discovery"
	echo "   2 - search for services"
	echo "   3 - simple server"
	echo "   4 - Luu Gia Thuy, simple android connection"
	exit 2
fi

if [ $1 -eq 1 ]
then
	echo "Simple Discovery Test"
	java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/test.bluecove.SimpleDiscovery
elif [ $1 -eq 2 ]
then
	echo "Services Discovery Test"
	java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/test.bluecove.ServicesSearch
elif [ $1 -eq 3 ]
then
	echo "Simple Server Test"
	java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/test.bluecove.SimpleServer
else
	echo "Data Exchange with Android"
	java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/bluecove.bluetooth.BluetoothManager
fi