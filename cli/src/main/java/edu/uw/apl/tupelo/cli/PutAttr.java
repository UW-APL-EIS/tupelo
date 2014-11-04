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

public class PutAttr extends CliBase {

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
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		String usage = commonUsage() + "diskID sessionID key valueFile";

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

		args = cl.getArgs();
		if( args.length < 4 ) {
			printUsage( os, usage, HEADER, FOOTER );
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
		Store store = Utils.buildStore( storeLocation );

		Collection<ManagedDiskDescriptor> stored = store.enumerate();
		System.out.println( "ManagedDisks: " + stored );

		ManagedDiskDescriptor mdd = Utils.locateDescriptor( store,
															diskID, sessionID );
		if( mdd == null ) {
			System.err.println( "Not stored: " + diskID + "," + sessionID );
			System.exit(1);
		}
		byte[] ba = FileUtils.readFileToByteArray( valueFile );
		System.out.println( "Storing attribute " + key +
							" for managedDisk " + mdd );
		store.setAttribute( mdd, key, ba );

		Collection<String> keys = store.listAttributes( mdd );
		System.out.println( "Stored Attributes: " + keys );
	}

	String diskID, sessionID;
	String key;
	File valueFile;

}

// eof
