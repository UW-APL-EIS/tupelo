package edu.uw.apl.tupelo.store.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

/**
 * A container for state common to all command line-driven tools in
 * this module
 */

class Base {
	
	protected Base() {
		storeLocation = STORELOCATIONDEFAULT;
		log = Logger.getLogger( getClass() );
	}

	protected void readArgs( String[] args ) {
		Options os = commonOptions();

		String usage = commonUsage() + " (diskID sessionID)?";
		final String HEADER = "";
		final String FOOTER = "";
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		commonParse( os, cl, usage, HEADER, FOOTER );
		if( all )
			return;
		args = cl.getArgs();
		if( args.length < 2 ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
	}

	static protected void printUsage( Options os, String usage,
									  String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	protected Options commonOptions() {
		Options os = new Options();
		os.addOption( "a", false,
					  "Hash all managed disks (those done not re-computed)" );
		os.addOption( "d", false, "Debug" );
		os.addOption( "h", false, "Help" );
		os.addOption( "s", true,
					  "Store directory. Defaults to " + STORELOCATIONDEFAULT );
		os.addOption( "v", false, "Verbose" );
		return os;
	}
	
	protected String commonUsage() {
		return getClass().getName() + " [-a] [-d] [-h] [-s storeLocation] [-v]";
	}
	
	protected void commonParse( Options os, CommandLine cl, String usage,
								String header, String footer ) {
		boolean help = cl.hasOption( "h" );
		if( help ) {
			printUsage( os, usage, header, footer );
			System.exit(1);
		}
		all = cl.hasOption( "a" );
		debug = cl.hasOption( "d" );
		verbose = cl.hasOption( "v" );
		if( debug )
			log.setLevel( Level.DEBUG );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
	}

	
	protected String storeLocation;
	protected FilesystemStore store;
	protected boolean all;
	protected String diskID, sessionID;
	protected Logger log;

	static protected final String STORELOCATIONDEFAULT = "./test-store";
	
	static protected boolean debug, verbose;
}

// eof
