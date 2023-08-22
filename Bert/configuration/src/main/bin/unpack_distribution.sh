#!/bin/sh
# Take the build product and unpack it
DIST=${PWD}/../bertApp/build/distributions
echo $DIST
mkdir -p ${BERT_HOME}/distribution
cd ${BERT_HOME}/distribution
tar -xvf ${DIST}/bertApp.tar

echo "Unpack distribution complete"