#!/bin/bash
# Working directory is the project directory.
# The important step here is wnen we change the execution path
# to reference the arm compilation tools. 
export PATH=$PATH:/usr/local/bin
if [ -d core ]
then
	cd core
fi


MAKEFILE="`pwd`/Makefile"
export GCC_DIR="${HOME}/opt/gcc-arm-none-eabi-7-2018-q2"
export PATH="$GCC_DIR}/arm-none-eabi/bin:${PATH}"
export PROJECT_ROOT="../../Core"
export BUILD_DIR="`pwd`/build"

mkdir -p ${BUILD_DIR}
cd ${PROJECT_ROOT}
make -e -f ${MAKEFILE} clean
make -e -f ${MAKEFILE}
make -e -f ${MAKEFILE} install
