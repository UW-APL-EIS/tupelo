Tupelo - A Disk Management System

Prerequisites
-------------

Tupelo is Java code, organized to build using Maven.  Tupelo is a
'multi-module' Maven codebase.  Being Java and using Maven, Tupelo has
two very obvious tool prerequisities:

A 1.7+ version of the Java Development Kit (JDK).

Apache Maven 3.0.5 or greater (earlier 2.* versions may work fine, but
3.0.5 is our current reference). See install instructions at
http://maven.apache.org/download.cgi.

After installation of both tools, run 'mvn -v' which shows Maven's
version and which JDK it has located.

Dependencies
------------

Tupelo dependencies (i.e. 3rd party code it builds against) include 

* fuse4j (Java-enabled fuse filesystems)

* tsk4j (Java-enabled Sleuthkit disk forensics software)

* native-lib-loader (loads JNI C code from classpath. Used by
  fuse4j,tsk4j)

These artifacts (jars) are not yet available on public facing Maven
repositories (i.e Central), and neither is their source code at any
public git location.

To overcome these temporary hurdles, the binary artifacts (jars) are
included here, see the ./repos directory.  These can be installed into
your local Maven repository (~/.m2/repository) via a provided script:

$ cd /path/to/tupelo

$ ./install-repos.sh

This script uses Maven itself so Maven must be installed first, see above.

Install
-------

Once the artifacts above are in your local Maven repo, Tupelo build can proceed:

$ cd /path/to/tupelo

$ mvn install

That's it!



