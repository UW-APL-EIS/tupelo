package edu.uw.apl.tupelo.cli;

import java.util.Collection;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

/**
 * Simple Tupelo Utility: Query and store for its contents
 * (ManagedDisks and Attributes) and print results to stdout.
 */

public class StoreInfo {

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
	}

	public StoreInfo() {
		storeLocation = Utils.STORELOCATIONDEFAULT;
	}

	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	public void readArgs( String[] args ) {
		Options os = Utils.commonOptions();
		os.addOption( "d", false, "Debug" );

		final String USAGE =
			HashData.class.getName() + " [-d] [-s storeLocation]";
		final String HEADER = "";
		final String FOOTER = "";
		
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		debug = cl.hasOption( "d" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
		args = cl.getArgs();
	}
	
	public void start() throws Exception {

		Store store = Utils.buildStore( storeLocation );
		if( debug )
			System.out.println( "Store type: " + store );

		Collection<ManagedDiskDescriptor> stored = store.enumerate();
		System.out.println( "ManagedDisks: " + stored );

		for( ManagedDiskDescriptor mdd : stored ) {
			Collection<String> attrNames = store.attributeSet( mdd );
			System.out.println( "Attributes for " + mdd + ": " + attrNames );
		}			
	}
	
	String storeLocation;
	static boolean debug;
}

// eof
