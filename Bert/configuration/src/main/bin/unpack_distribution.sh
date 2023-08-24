#!/bin/sh

# Take the build product and unpack it
DIST=${PWD}/../bertApp/build/distributions
echo "Unpack " $DIST
mkdir -p ${BERT_HOME}/distribution
cd ${BERT_HOME}/distribution
tar -xf ${DIST}/bertApp.tar

echo "Unpack complete"