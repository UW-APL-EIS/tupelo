package edu.uw.apl.tupelo.cli;

import org.apache.commons.cli.*;
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

	protected Options commonOptions() {
		Options os = Utils.commonOptions();
		return os;
	}
	
	protected String commonUsage() {
		return getClass().getName() + " [-d] [-h] [-s storeLocation]";
	}
	
	protected void commonParse( Options os, CommandLine cl, String usage,
								String header, String footer ) {
		boolean help = cl.hasOption( "h" );
		if( help ) {
			printUsage( os, usage, header, footer );
			System.exit(1);
		}
		debug = cl.hasOption( "d" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
	}
	
	protected String storeLocation;
	protected Logger log;

	
	static protected boolean debug;
}

// eof
