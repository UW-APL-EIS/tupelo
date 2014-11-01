package edu.uw.apl.tupelo.cli;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

/**
 * A container for state common to all command line tools in this module
 */

public class CliBase {

	protected CliBase() {
		storeLocation = Utils.STORELOCATIONDEFAULT;
		log = Logger.getLogger( getClass() );
	}

	static protected void printUsage( Options os, String usage,
									  String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	protected String storeLocation;
	protected Logger log;

	static protected boolean debug;
}

// eof
