package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.null_.NullStore;

/**
 * Simple Tupelo Utility: add some arbitrary disk image file to a
 * local Tupelo Filesystem-based Store. To use:
 *
 * PutData imageFile
 *
 * see the supported options, below
 */

public class PutData extends CliBase {

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
		// Will call close() on all loggers which will shutdown any amqp stuff.
		LogManager.shutdown();
	}

	public PutData() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "n", false,
					  "Dryrun, use null store (like /dev/null)" );
		os.addOption( "f", false,
					  "Force flat managed disk, default decides based on unmanaged data size" );
		os.addOption( "o", false,
					  "Force stream-optimized managed disk, default decides based on unmanaged data size" );
		String usage = commonUsage() + "[-f] [-o] /path/to/unmanagedData";

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

		dryrun = cl.hasOption( "n" );
		forceFlatDisk = cl.hasOption( "f" );
		forceStreamOptimizedDisk = cl.hasOption( "o" );
		args = cl.getArgs();
		if( args.length > 0 ) {
			rawData = new File( args[0] );
			if( !rawData.exists() ) {
				// like bash would do, write to stderr...
				System.err.println( rawData + ": No such file or directory" );
				System.exit(-1);
			}
		} else {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
	}
	
	public void start() throws IOException {
		Store s = null;
		if( dryrun ) {
			s = new NullStore();
			log.info( getClass() + " " + storeLocation );
		} else {
			s = Utils.buildStore( storeLocation );
			log.info( getClass() + " " + storeLocation );
		}
		if( debug )
			System.out.println( "Store: " + s );
		

		System.out.println( "Store.usableSpace: " + s.getUsableSpace() );
		Collection<ManagedDiskDescriptor> mdds1 = s.enumerate();
		System.out.println( "Stored data: " + mdds1 );

		Session session = s.newSession();
		final UnmanagedDisk ud = new DiskImage( rawData );
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		System.out.println( "Storing: " + mdd +
							" (" + ud.size() + " bytes)" );

		ManagedDisk md = null;
		if( forceFlatDisk ) {
			md = new FlatDisk( ud, session );
		} else if( forceStreamOptimizedDisk ) {
			md = new StreamOptimizedDisk( ud, session );
		} else {
			if( ud.size() < 1024L * 1024 * 1024 ) {
				md = new FlatDisk( ud, session );
			} else {
				md = new StreamOptimizedDisk( ud, session );
			}
		}
		ProgressMonitor.Callback cb = new ProgressMonitor.Callback() {
				@Override
				public void update( long in, long out, long elapsed ) {
					double pc = in / (double)ud.size() * 100;
					System.out.print( (int)pc + "% " );
					System.out.flush();
					if( in == ud.size() )
						System.out.println();
				}
			};
		s.put( md, cb, 5 );

		Collection<ManagedDiskDescriptor> mdds2 = s.enumerate();
		System.out.println( "Stored data: " + mdds2 );
	}


	boolean forceFlatDisk, forceStreamOptimizedDisk, dryrun;
	File rawData;
}

// eof
