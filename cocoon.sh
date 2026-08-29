#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

CWD=`pwd`
MAVEN_OPTS="$MAVEN_OPTS -Djava.awt.headless=true -Dorg.apache.cocoon.mode=dev -Djetty.http.port=8888"

ARGS=""
while [ "$#" -gt "0" ]
do
  case "$1" in
    debug)
      MAVEN_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n $MAVEN_OPTS -Djava.compiler=NONE"
      ;;
    *)
      ARGS="$ARGS $1"
  esac

  shift
done

export MAVEN_OPTS

# JDK 17 is the baseline.
if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME=`/usr/libexec/java_home -v 17 2>/dev/null`
  export JAVA_HOME
fi

# Jetty 12 / Jakarta EE 10. The old `jetty:run` prefix resolved to the javax-only
# org.eclipse.jetty:jetty-maven-plugin and no longer works here.
#
# run-war, not run: `run` adds the project's dependency classpath on top of the already
# assembled WEB-INF/lib, and Jetty then rejects the duplicated spring-web web-fragment.
#
# A fresh temp directory per start: Cocoon's pipeline cache lives there, and reusing it
# after block content has changed can serve one block's resource for another block's URL.
COCOON_TMP=`mktemp -d`

echo "Starting in `pwd` (tmpdir $COCOON_TMP)"
mvn -P samples -pl core/cocoon-webapp \
    org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.16:run-war \
    -Djetty.http.port=8888 -Djava.io.tmpdir="$COCOON_TMP"

cd $CWD
