#!/bin/sh

echo
echo "Cocoon Build System"
echo "-------------------"
echo

if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi

LOCALCLASSPATH=$JAVA_HOME/lib/tools.jar:./lib/xerces_1_0_1.jar:./lib/xalan_0_19_2.jar:./lib/fop_0_12_0.jar:./lib/servlet_2_2.jar:./lib/ant.jar:./lib/xml.jar

echo Building with classpath $CLASSPATH:$LOCALCLASSPATH
echo

echo Starting Ant...
echo

$JAVA_HOME/bin/java -classpath $CLASSPATH:$LOCALCLASSPATH org.apache.tools.ant.Main $*
