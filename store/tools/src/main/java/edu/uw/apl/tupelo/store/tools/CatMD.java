package edu.uw.apl.tupelo.store.tools;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

/**
 * Simply open the identified managed disk and stream its entire
 * contents to stdout.  Really just tests the
 * ManagedDisk.getInputStream and readImpls.
 */

public class CatMD extends Base {

	static public void main( String[] args ) {
		CatMD main = new CatMD();
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

	public CatMD() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "v", false, "Verbose" );

		String usage = commonUsage() + " [-v] (diskID sessionID)?";
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

		verbose = cl.hasOption( "v" );
		args = cl.getArgs();
		if( args.length < 2 ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
	}
	
	public void start() throws Exception {
		File dir = new File( storeLocation );
		if( !dir.isDirectory() ) {
			throw new IllegalStateException
				( "Not a directory: " + storeLocation );
		}
		FilesystemStore store = new FilesystemStore( dir );
		if( debug )
			System.out.println( "Store type: " + store );
		ManagedDiskDescriptor mdd = locateDescriptor( store,
													  diskID, sessionID );
		if( mdd == null ) {
			System.err.println( "Not stored: " + diskID + "," + sessionID );
			System.exit(1);
		}
		ManagedDisk md = store.locate( mdd );
		InputStream is = md.getInputStream();
		byte[] ba = new byte[1024*4];
		while( true ) {
			int nin = is.read( ba );
			if( nin < 1 )
				break;
			System.out.write( ba, 0, nin );
		}
	}

	String diskID, sessionID;
	static boolean verbose;
}

// eof
