#!/usr/bin/env sh
# Gradle wrapper script (standard boilerplate)
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi
exec "$JAVACMD" "$@"
