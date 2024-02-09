#!/bin/bash

JAVAC="$JDK_HOME/bin/javac"
JAR="$JDK_HOME/bin/jar"
JAVADOC="$JDK_HOME/bin/javadoc"

if [[ -z "$CLASSPATH" ]]
then
  echo "Variable CLASSPATH not set"
  exit 1
fi

rm -rf ./out 2> /dev/null
mkdir out

echo "Compiling java library"
cd src
JAVAC_OUTPUT=$($JAVAC -cp "../lib/win-x64/jarqanore.jar;../lib/gson-2.9.1.jar" -d ../out -g `find . -type f -name "*.java"` 2>&1)
JAVAC_OUTPUT_EXITCODE=$?

if [[ JAVAC_OUTPUT_EXITCODE -ne 0 ]];
then
	echo "$JAVAC_OUTPUT"
	exit $JAVAC_OUTPUT_EXITCODE
fi

echo "Generating docs"
JAVADOC_OUTPUT=$($JAVADOC -cp "../lib/win-x64/jarqanore.jar;../lib/gson-2.9.1.jar" -d ../out/doc/ `find . -type f -name "*.java"` 2>&1)
JAVADOC_OUTPUT_EXITCODE=$?

if [[ JAVADOC_OUTPUT_EXITCODE -ne 0 ]];
then
	echo "$JAVADOC_OUTPUT"
	exit $JAVADOC_OUTPUT_EXITCODE
fi
cd ..

echo "Generating jar"
cd out
JAR_OUTPUT=$($JAR -cf arqanite.jar ./be/ 2>&1)
JAR_OUTPUT_EXITCODE=$?

if [[ JAR_OUTPUT_EXITCODE -ne 0 ]];
then
	echo "$JAR_OUTPUT"
	exit $JAR_OUTPUT_EXITCODE
fi
cd ..

echo "Cleaning up"
cd out
rm -rf ./be
