#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Standard Gradle wrapper launcher script.
# If Android Studio prompts to regenerate/repair the wrapper on first sync,
# accept — this project intentionally ships without the wrapper jar binary.
#

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

APP_HOME=$(cd "$(dirname "$0")" >/dev/null && pwd)

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] ; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
