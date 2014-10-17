package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;

/**
 * Simple Tupelo Utility: Hash some previously added ManagedDisk,
 * using tsk4j/Sleuthkit routines and a FUSE filesystem to access the
 * managed data.  Store the resultant 'Hash Info' as a Store
 * attribute.
 */

public class HashData {

	static public void main( String[] args ) {
		HashData main = new HashData();
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

	public HashData() {
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
		os.addOption( "d", false, "debug on" );
		os.addOption( "s", true, "Store location, defaults to " +
					  STORELOCATIONDEFAULT );

		final String USAGE =
			HashData.class.getName() + " [-s storeLocation] diskID sessionID";
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
		if( args.length < 2 ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
	}
	
	public void start() throws Exception {
		File dir = new File( storeLocation );
		Store store = new FilesystemStore( dir );
		ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		File mountPoint = new File( "test-mount" );
		mountPoint.mkdirs();
		mdfs.mount( mountPoint, true );
		Thread.sleep( 1000 * 20 );

		
		mdfs.umount();
	}


	String storeLocation;
	String diskID, sessionID;
	static boolean debug;
	
	static final String STORELOCATIONDEFAULT = "./test-store";
}

// eof
