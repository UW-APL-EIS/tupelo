package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;

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
		} finally {
			LogManager.shutdown();
		}
	}

	public PutAttr() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "f", true, "valueFile" );
		String usage = commonUsage() +
			"diskID sessionID key [-f valueFile | valueString]";

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
		if( cl.hasOption( "f" ) ) {
			String valueFileName = cl.getOptionValue( "f" );
			valueFile = new File( valueFileName );
			if( !( valueFile.isFile() && valueFile.canRead() ) ) {
				System.err.println( valueFile + ": No such file" );
				System.exit(-1);
			}
		}
		args = cl.getArgs();
		int requiredArgs = valueFile == null ? 4 : 3;
		if( args.length < requiredArgs ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
		key = args[2];
		if( valueFile == null )
			valueString = args[3];
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
		byte[] ba = null;
		if( valueFile != null ) {
			ba = FileUtils.readFileToByteArray( valueFile );
		} else {
			ba = valueString.getBytes();
		}
		System.out.println( "Storing attribute " + key +
							" for managedDisk " + mdd );
		store.setAttribute( mdd, key, ba );

		Collection<String> keys = store.listAttributes( mdd );
		System.out.println( "Stored Attributes: " + keys );
	}

	String diskID, sessionID;
	String key;
	File valueFile;
	String valueString;
}

// eof
