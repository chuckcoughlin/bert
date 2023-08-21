#!/bin/sh
# Take the build product and unpack it
DIST=${PWD}/../app/build/distributions/app
mkdir -p ${BERT_HOME}/distribution
cd ${BERT_HOME}/distribution
tar -xvf ${DIST}/app.tar
