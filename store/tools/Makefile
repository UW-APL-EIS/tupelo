INSTALLROOT ?= $(HOME)/local

BINDIR = $(INSTALLROOT)/bin

JARSDIR = $(INSTALLROOT)/jars

default: install

install: install-bin install-jars

install-bin:
	-mkdir -p $(BINDIR)
	cp src/main/bash/tup.storebase $(BINDIR)
	cp src/main/bash/tup.digest $(BINDIR)
	cp src/main/bash/tup.bodyfile $(BINDIR)
	cp src/main/bash/tup.hashfs $(BINDIR)
	cp src/main/bash/tup.hashvs $(BINDIR)

install-jars:
	-mkdir -p $(JARSDIR)
	mvn clean package
	cp target/*.jar $(JARSDIR)

# eof
