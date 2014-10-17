package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

/**
 * Simple Tupelo Utility: Add an attribute to a local Tupelo
 * Filesystem-based Store. The attribute contents are taken from a
 * supplied local file.
 */

public class PutAttr {

	static public void main( String[] args ) {
		PutAttr main = new PutAttr();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			System.exit(-1);
		}
	}

	public PutAttr() {
		storeLocation = STORELOCATIONDEFAULT;
	}

	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	public void readArgs( String[] args ) {
		Options os = new Options();
		os.addOption( "s", true, "Store location, defaults to " +
					  STORELOCATIONDEFAULT );

		final String USAGE =
			PutAttr.class.getName() + " [-s storeLocation] diskID sessionID key valueFile";
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
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
		args = cl.getArgs();
		if( args.length < 4 ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
		key = args[2];
		valueFile = new File( args[3] );
		if( !valueFile.exists() ) {
			// like bash would do, write to stderr...
			System.err.println( valueFile + ": No such file or directory" );
			System.exit(-1);
		}
	}
	
	public void start() throws IOException {
		File dir = new File( storeLocation );
		Store store = new FilesystemStore( dir );

		Collection<ManagedDiskDescriptor> stored = store.enumerate();
		System.out.println( "Stored: " + stored );

		ManagedDiskDescriptor mdd = Utils.locateDescriptor( store, diskID, sessionID );
		if( mdd == null ) {
			System.err.println( "Not stored: " + diskID + "," + sessionID );
			System.exit(1);
		}
		byte[] ba = FileUtils.readFileToByteArray( valueFile );
		store.setAttribute( mdd, key, ba );

		Collection<String> keys = store.attributeSet( mdd );
		System.out.println( "Stored Attributes: " + keys );
	}


	String storeLocation;
	String diskID, sessionID;
	String key;
	File valueFile;

	static final String STORELOCATIONDEFAULT = "./test-store";
}

// eof
