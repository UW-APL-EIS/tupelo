Tupelo - Software for Whole Disk Drive Capture
==============================================

Tupelo is Java software for capturing the state of an entire hard
drive and saving a copy of that state offline where it can be
processed using forensics tools such as Sleuthkit.  The capture
produces a logical rather than physical copy of the original disk, so
space is saved at the offline site.

Tupelo is designed for a drive (in fact, many drives) to be
imaged/captured many times.  The space efficiency of the captured
'snapshots' increases as the capture count increases.  Imaging a 300GB
drive 5 times will cost you far less than the 1.5TB that would be
needed for true bit-for-bit copies.  Furthermore, the space-compressed
offline copy is still fully usable in-place.  There is no need to
'inflate back to raw' so that e.g. Sleuthkit tools can walk the
captured drive.

Prerequisites
-------------

Tupelo is Java code, organized to build using Maven.  Tupelo is a
'multi-module' Maven codebase.  Being Java and using Maven, Tupelo has
two very obvious tool prerequisites:

A 1.7+ version of the Java Development Kit (JDK).  For installation on Ubuntu:

$ sudo apt-get install openjdk-7-jdk

will install the OpenJDK toolset.  You may prefer the Sun/Oracle
toolset, but that takes more work to install. See
e.g. http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html.

Apache Maven 3.0.5 or greater (earlier 2.* versions may work fine, but
3.0.5 is our current reference). See install instructions at
http://maven.apache.org/download.cgi.  For quick install on Ubuntu:

$ sudo apt-get install maven

After installation of both tools, run 'mvn -v' which shows Maven's
version and which JDK it has located.  You are hoping to see something
very close to this:

$ mvn -v

Apache Maven 3.0.4
Maven home: /usr/share/maven
Java version: 1.7.0_65, vendor: Oracle Corporation
Java home: /usr/lib/jvm/java-7-openjdk-amd64/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "3.11.0-15-generic", arch: "amd64", family: "unix"

Install
-------

```
$ cd /path/to/tupelo-git-repo

$ mvn install
```

will compile and package up all the Java code into what Maven calls
'artifacts', which are just carefully named Java jar files.  The
artifacts are then copied to your local Maven repo.

```
$ mvn javadoc:aggregate
```

The Javadoc APIs should then be available at ./target/site/apidocs.

There are unit tests for some modules.  These are only run when the
'tester' profile is activated.  If you want to run unit tests, try:

```
$ mvn test -Ptester
```

which will run all the unit tests.  The unit tests for the http/client
sub-module will fail unless you first fire up a 'Tupelo web-based
store', like this (in a different terminal)

$ cd /path/to/tupelo-git-repo/http/server

$ mvn jetty:run

which spawns the Jetty web container to host the Tupelo web-based
store.  The http/client unit tests then access this store via
url base http://localhost:8888/tupelo/

Modules
-------

The Tupelo codebase is organised as several Maven 'modules', with a
parent pom at the root level.  The important modules are as follows

* model

* store

* cli

# Command Line Interface (CLI)

To use Tupelo, we run the code in the cli module.  Inspired by git,
Tupelo uses a 'tup' driver program (a shell script invoking the Java
code) with many sub-commands. After building the code from source (see
above), we are ready to try it out.


```
$ cd /path/to/tupelo/cli

$ ./tup
```


Local Repository
----------------

The Tupelo artifacts built here themselves depend on various
existing Maven artifacts which are not (yet) available on a public
Maven repository (like Maven Central).  Some of dependencies are
available in source form on github.  But to save the Tupelo builder
the effort of building all these dependencies from source, we include a
'project-local' Maven repository so that the Tupelo artifacts will
build.  We can inspect this local repository thus:

```
$ cd /path/to/tupelo

$ tree ./repository
```


# Video/Slides

Ideas related to this work were presented at the [OSDFCon]
(http://www.osdfcon.org/2016-event/) workshop in October 2016.  A local copy
of the slides is also included [here](./doc/MacleanTupeloOSDF2016.pdf).

# Contact

stuart at apl dot washington dot edu

