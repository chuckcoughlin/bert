#!/bin/sh
# Remove old log files in ${BERT_HOME}/logs
mkdir -p ${BERT_HOME}/logs
cd ${BERT_HOME}/logs
rm -f bert.* blueserver.log.bak
