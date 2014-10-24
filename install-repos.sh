#!/bin/sh

# Copy into our local Maven repo those Tupelo dependencies 
# for which we may not have sources.  We need a Maven repository!

mvn -q install:install-file -Dfile=repos/native-lib-loader-1.0.1.jar

mvn -q install:install-file -Dfile=repos/fuse4j-core-2400.0.0.jar

mvn -q install:install-file -Dfile=repos/tsk4j-core-413.0.0.jar

# eof
