#!/bin/sh

echo
echo "Cocoon Build System"
echo "-------------------"

if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi

ANT_HOME=./lib
ANT=./lib/ant.jar
JAVAC=$JAVA_HOME/lib/tools.jar
XERCES=./lib/xerces_1_0_3.jar
XALAN=./lib/xalan_1_0_1.jar
FOP=./lib/fop_0_12_1.jar
SERVLETS=./lib/servlet_2_2.jar
LOCALCLASSPATH=$ANT:$JAVAC:$XERCES:$XALAN:$FOP:$SERVLETS:$CLASSPATH

echo
echo Building with classpath $LOCALCLASSPATH

echo
echo Starting Ant...

$JAVA_HOME/bin/java -Dant.home=$ANT_HOME -classpath $LOCALCLASSPATH org.apache.tools.ant.Main $*
