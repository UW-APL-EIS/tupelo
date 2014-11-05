# Makefile for installing Tupelo components into the /opt/dims
# filesystem structure.  Components here means 
# 
# 1: Java JAR files
#
# 2: Shell scripts driving those JARs.

SHELL=/bin/bash

DIMSHOME=/opt/dims

JARDIR=$(DIMSHOME)/jars

BINDIR=$(DIMSHOME)/bin

OWNER = dims
GROUP = dims
MODE  = 755

INSTALL=install -g $(GROUP) -o $(OWNER) -m $(MODE)

.PHONY: help
help:
	@echo "help not available (yet)"

.PHONY: install
install: install-jars install-bin

.PHONY: install-jars
install-jars: package installdirs
	@$(INSTALL) cli/target/*.jar $(JARDIR)

.PHONY: install-bin
install-bin: installdirs
	@$(INSTALL) bin/* $(BINDIR)

.PHONY: package
package:
	mvn package

installdirs:
	[ -d $(JARDIR) ] || \
	(mkdir -p $(JARDIR); \
	chown dims:dims $(JARDIR); \
	chmod 755 $(JARDIR); \
	echo "Created $(JARDIR) (dims:dims, mode 755)")
	[ -d $(BINDIR) ] || \
	(mkdir -p $(BINDIR); \
	chown dims:dims $(BINDIR); \
	chmod 755 $(BINDIR); \
	echo "Created $(BINDIR) (dims:dims, mode 755)")

.PHONY: vars
vars:
	@echo JARDIR  is $(JARDIR)
	@echo BINDIR  is $(BINDIR)
	@echo OWNER   is $(OWNER)
	@echo GROUP   is $(GROUP)
	@echo MODE    is $(MODE)
	@echo INSTALL is $(INSTALL)

# eof
