#!/bin/sh

# Copy into our local Maven repo (~/.m2/repositoty) those Tupelo
# dependencies for which we may not have sources.  

ARTIFACTS=`ls repos/*.jar`

for a in $ARTIFACTS
do
    echo Installing $a
    mvn -q install:install-file -Dfile=$a
done

# eof
