#!/bin/sh
# Execute canned bluecove tests
# Requires access to BERT_HOME
cd ${BERT_HOME}
mkdir -p logs

MP=lib/bluecove-2.1.0a.jar

# Allow debugging on port 8000
X="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"

echo "Simple Discovery Test"
java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/test.bluecove.SimpleDiscovery

echo "Services Discovery Test"
java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/test.bluecove.ServicesSearch

echo "Simple Server Test"
java $X --module-path $MP -Djava.library.path=/usr/local/robot/lib -m bluecove/test.bluecove.SimpleServer