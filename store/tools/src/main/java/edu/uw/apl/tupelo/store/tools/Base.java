package edu.uw.apl.tupelo.store.tools;

import java.io.IOException;

import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

/**
 * A container for state common to all command line-driven tools in
 * this module
 */

public class Base {

	protected Base() {
		storeLocation = STORELOCATIONDEFAULT;
		log = Logger.getLogger( getClass() );
	}

	static protected void printUsage( Options os, String usage,
									  String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	protected Options commonOptions() {
		Options os = new Options();
		os.addOption( "d", false, "Debug" );
		os.addOption( "h", false, "Help" );
		os.addOption( "s", true,
					  "Store directory. Defaults to " + STORELOCATIONDEFAULT );
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
		if( debug )
			log.setLevel( Level.DEBUG );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
	}

	public ManagedDiskDescriptor locateDescriptor( Store s,
												   String diskID,
												   String sessionID )
		throws IOException {
		for( ManagedDiskDescriptor mdd : s.enumerate() ) {
			if( mdd.getDiskID().equals( diskID ) &&
				mdd.getSession().toString().equals( sessionID ) ) {
				return mdd;
			}
		}
		return null;
	}
	
	protected String storeLocation;
	protected Logger log;

	static protected final String STORELOCATIONDEFAULT = "./test-store";
	
	static protected boolean debug;
}

// eof
