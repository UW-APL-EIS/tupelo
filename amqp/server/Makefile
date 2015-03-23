INSTALLROOT ?= $(HOME)/local

BINDIR = $(INSTALLROOT)/bin

JARSDIR = $(INSTALLROOT)/jars

default: install

install: install-bin install-jars

install-bin:
	-mkdir -p $(BINDIR)
	cp src/main/bash/tup.amqps $(BINDIR)

install-jars:
	-mkdir -p $(JARSDIR)
	mvn clean package
	cp target/*.jar $(JARSDIR)

# eof
