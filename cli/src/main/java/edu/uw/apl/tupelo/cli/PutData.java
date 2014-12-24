package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.model.PhysicalDisk;
import edu.uw.apl.tupelo.model.VirtualDisk;
import edu.uw.apl.tupelo.store.Store;

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
		} finally {
			/*
			  Will call close() on all loggers which will
			  shutdown any amqp stuff.
			*/
			  LogManager.shutdown();
		}
	}

	public PutData() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "n", false,
					  "Dryrun, use null store (like /dev/null)" );
		os.addOption( "f", false,
					  "Force flat managed disk, default based on unmanaged size" );
		os.addOption( "o", false,
					  "Force stream-optimized managed disk, default based on unmanaged size" );
		os.addOption( "q", false,
					  "Quiet, do not print progress" );
		String usage = commonUsage() + " [-f] [-o] [-n] [-q] /path/to/unmanagedData";

		final String HEADER =
			"Transfer an unmanaged disk image to a Tupelo store.";
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
		quiet = cl.hasOption( "q" );
		args = cl.getArgs();
		if( args.length == 1 ) {
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
		Store store = Utils.buildStore( storeLocation );
		log.info( store.getClass() + " " + storeLocation );
		if( debug )
			System.out.println( "Store: " + store );
		
		try {
			System.out.println( "Store.usableSpace: " + store.getUsableSpace() );
		} catch( ConnectException ce ) {
			System.err.println( "Network Error. Remote Tupelo store up?" );
			System.exit(0);
		}

		UnmanagedDisk ud = null;
		if( false ) {
		} else if( rawData.getPath().startsWith( "/dev/" ) ) {
			ud = new PhysicalDisk( rawData );
		} else if( VirtualDisk.likelyVirtualDisk( rawData ) ) {
			ud = new VirtualDisk( rawData );
		} else {
			ud = new DiskImage( rawData );
		}

		Collection<ManagedDiskDescriptor> existing = store.enumerate();
		if( verbose )
			System.out.println( "Stored data: " + existing );

		List<ManagedDiskDescriptor> matching =
			new ArrayList<ManagedDiskDescriptor>();
		for( ManagedDiskDescriptor mdd : existing ) {
			if( mdd.getDiskID().equals( ud.getID() ) ) {
				matching.add( mdd );
			}
		}
		Collections.sort( matching, ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		System.out.println( "Matching data: " + matching );
		
		ManagedDiskDigest digest = null;
		UUID uuid = null;
		if( !matching.isEmpty() ) {
			ManagedDiskDescriptor recent = matching.get( matching.size()-1 );
			log.info( "Retrieving uuid for: "+ recent );
			uuid = store.uuid( recent );
			System.out.println( "UUID: " + uuid );
			log.info( "Retrieving digest for: "+ recent );
			digest = store.digest( recent );
			System.out.println( "Digest: " + digest.size() );
			
		}

		if( dryrun )
			return;
		
		Session session = store.newSession();
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		System.out.println( "Storing: " + rawData +
							" (" + ud.size() + " bytes) to " + mdd );

		ManagedDisk md = null;

		boolean useFlatDisk = false;
		if( forceFlatDisk ) {
			useFlatDisk = true;
		} else if( forceStreamOptimizedDisk ) {
			useFlatDisk = false;
		} else {
			useFlatDisk = ud.size() < 1024L * 1024 * 1024;
		}

		if( useFlatDisk ) {
			md = new FlatDisk( ud, session );
		} else {
			if( uuid != null )
				md = new StreamOptimizedDisk( ud, session, uuid );
			else
				md = new StreamOptimizedDisk( ud, session );
			md.setCompression( ManagedDisk.Compressions.SNAPPY );
		}

		if( digest != null )
			md.setParentDigest( digest );
		
		if( quiet ) {
			store.put( md );
		} else {
			final long sz = ud.size();
			ProgressMonitor.Callback cb = new ProgressMonitor.Callback() {
					@Override
					public void update( long in, long out, long elapsed ) {
						double pc = in / (double)sz * 100;
						System.out.print( (int)pc + "% " );
						System.out.flush();
						if( in == sz ) {
							System.out.println();
							System.out.printf( "Unmanaged size: %12d\n",
											   sz );
							System.out.printf( "Managed   size: %12d\n", out );
							System.out.println( "Elapsed: " + elapsed );
						}
					}
				};
			store.put( md, cb, 5 );
		}

		/*
		  Add at least an attribute that identifies the file path, accepting
		  that that may not be universally identying, but helps in testing
		*/
		store.setAttribute( mdd, "path", rawData.getPath().getBytes() );
		
		if( verbose ) {
			Collection<ManagedDiskDescriptor> mdds2 = store.enumerate();
			System.out.println( "Stored data: " + mdds2 );
		}
	}


	boolean forceFlatDisk, forceStreamOptimizedDisk, dryrun;
	boolean quiet, verbose;
	File rawData;
}

// eof
