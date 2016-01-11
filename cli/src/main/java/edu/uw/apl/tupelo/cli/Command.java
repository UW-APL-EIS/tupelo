package edu.uw.apl.tupelo.cli;

/**
 * @author Stuart Maclean
 *
 * The main cmd line 'driver' for Tupelo operations.  Mimics how git
 * has a single cmd 'git', and uses subcommands.
 */

abstract public class Command {

	abstract public void invoke( String[] args );
}

// eof
