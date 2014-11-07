package edu.uw.apl.tupelo.cli;

import java.net.ConnectException;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

/**
 * Simple Tupelo Utility: Query and store for its contents
 * (ManagedDisks and Attributes) and print results to stdout.
 */

public class StoreInfo extends CliBase {

	static public void main( String[] args ) {
		StoreInfo main = new StoreInfo();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			if( debug )
				e.printStackTrace();
			System.exit(-1);
		}
		LogManager.shutdown();
	}

	public StoreInfo() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		String usage = commonUsage();
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
	}
	
	public void start() throws Exception {

		Store store = Utils.buildStore( storeLocation );
		if( debug )
			System.out.println( "Store: " + store );
		log.info( getClass() + " " + storeLocation );
		
		System.out.println( "Using store: " + storeLocation );
		System.out.println( "Usable Space: " + store.getUsableSpace() );

		Collection<ManagedDiskDescriptor> stored = null;
		try {
			stored = store.enumerate();
		} catch( ConnectException ce ) {
			System.err.println( "Network Error. Is the remote Tupelo store up?" );
			System.exit(0);
		}
		
		System.out.println( "ManagedDisks: " + stored );

		for( ManagedDiskDescriptor mdd : stored ) {
			Collection<String> attrNames = store.listAttributes( mdd );
			System.out.println( "Attributes for " + mdd + ": " + attrNames );
		}			
	}
	
	
}

// eof
