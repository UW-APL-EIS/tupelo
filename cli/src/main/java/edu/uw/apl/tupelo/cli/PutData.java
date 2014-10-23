package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;

/**
 * Simple Tupelo Utility: add some arbitrary disk image file to a
 * local Tupelo Filesystem-based Store.
 */

public class PutData {

	static public void main( String[] args ) {
		PutData main = new PutData();
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

	public PutData() {
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
		os.addOption( "d", false, "Debug" );
		os.addOption( "s", true,
					  "Store url/directory. If directory, defaults to " +
					  STORELOCATIONDEFAULT );
		os.addOption( "f", false,
					  "Force flatDisk, default decides based on unmanaged data size" );

		final String USAGE =
			PutData.class.getName() + " [-s storeLocation] [-f] unmanagedData";
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
		forceFlatDisk = cl.hasOption( "f" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
		args = cl.getArgs();
		if( args.length > 0 ) {
			rawData = new File( args[0] );
			if( !rawData.exists() ) {
				// like bash would do, write to stderr...
				System.err.println( rawData + ": No such file or directory" );
				System.exit(-1);
			}
		} else {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
	}
	
	public void start() throws IOException {

		Store s = null;
		if( storeLocation.startsWith( "http" ) ) {
			s = new HttpStoreProxy( storeLocation );
		} else {
			File dir = new File( storeLocation );
			if( !dir.isDirectory() ) {
				throw new IllegalStateException
					( "Not a directory: " + storeLocation );
			}
			s = new FilesystemStore( dir );
		}
		System.out.println( "Store type: " + s );
		
		System.out.println( "Store.usableSpace:" + s.getUsableSpace() );
		Collection<ManagedDiskDescriptor> mdds1 = s.enumerate();
		System.out.println( "Stored data: " + mdds1 );

		Session session = s.newSession();
		UnmanagedDisk ud = new DiskImage( rawData );
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		System.out.println( "Storing: " + mdd +
							"(" + ud.size() + " bytes)" );

		ManagedDisk md = null;
		if( forceFlatDisk || ud.size() < 1024L * 1024 * 1024 ) {
			md = new FlatDisk( ud, session );
		} else {
			md = new StreamOptimizedDisk( ud, session );
		}
		s.put( md );

		Collection<ManagedDiskDescriptor> mdds2 = s.enumerate();
		System.out.println( "Stored data: " + mdds2 );
	}


	boolean forceFlatDisk;
	String storeLocation;
	File rawData;

	static boolean debug;
	
	static final String STORELOCATIONDEFAULT = "./test-store";
}

// eof
