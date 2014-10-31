Tupelo - A Disk Management System

Prerequisites
-------------

Tupelo is Java code, organized to build using Maven.  Tupelo is a
'multi-module' Maven codebase.  Being Java and using Maven, Tupelo has
two very obvious tool prerequisities:

A 1.7+ version of the Java Development Kit (JDK).  For installation on Ubuntu:

$ sudo apt-get install openjdk-7-jdk

will install the OpenJDK toolset.  You may prefer the Sun/Oracle
toolset, but that takes more work to install. See
e.g. http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html

Apache Maven 3.0.5 or greater (earlier 2.* versions may work fine, but
3.0.5 is our current reference). See install instructions at
http://maven.apache.org/download.cgi.  For quick install on Ubuntu:

$ sudo apt-get install maven

After installation of both tools, run 'mvn -v' which shows Maven's
version and which JDK it has located.  You are hoping to see something very close to this:

$ mvn -v


Apache Maven 3.0.4
Maven home: /usr/share/maven
Java version: 1.7.0_65, vendor: Oracle Corporation
Java home: /usr/lib/jvm/java-7-openjdk-amd64/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "3.11.0-15-generic", arch: "amd64", family: "unix"

Install
-------

Once the artifacts above are in your local Maven repo, Tupelo build can proceed:

$ cd /path/to/tupelo

$ mvn install

That's it!


Dependencies (informational only, NOT required setup)
------------

Tupelo dependencies (i.e. 3rd party code it builds against) include 

* fuse4j (Java-enabled fuse filesystems)

* tsk4j (Java-enabled Sleuthkit disk forensics software)

* native-lib-loader (loads JNI C code from classpath. Used by
  fuse4j,tsk4j)

These artifacts (jars,poms) are not yet available on public facing
Maven repositories (i.e Central), so are bundled into a project-local
Maven repository at ./respository.  The modules that need these
dependencies (cli and fuse) include this local repository in their
pom.



