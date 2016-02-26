package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;
import edu.uw.apl.tupelo.store.null_.NullStore;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;

import edu.uw.apl.commons.devicefiles.DeviceFile;

public class PushCmd extends Command {
	PushCmd() {
		super( "push", "Push local device content to a Tupelo store" );
	}
	
	@Override
	public void invoke( String[] args ) throws Exception {
		Options os = commonOptions();
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
			commonParse( cl );
		} catch( ParseException pe ) {
			//	printUsage( os, usage, HEADER, FOOTER );
			//System.exit(1);
		}
		args = cl.getArgs();
		if( args.length < 2 ) {
			System.err.println( "Need device + store args" );
			return;
		}
		Config c = new Config();
		c.load( config );
		
		String deviceName = args[0];
		Config.Device selectedDevice = null;
		for( Config.Device d : c.devices() ) {
			if( d.getName().equals( deviceName ) ) {
				selectedDevice = d;
				break;
			}
		}
		if( selectedDevice == null ) {
			System.err.println( "'" + deviceName + "' is not a device" );
			return;
		}
		UnmanagedDisk ud = createUnmanagedDisk( selectedDevice );

		String storeName = args[1];
		Config.Store selectedStore = null;
		for( Config.Store cs : c.stores() ) {
			if( cs.getName().equals( storeName ) ) {
				selectedStore = cs;
				break;
			}
		}
		if( selectedStore == null ) {
			System.err.println( "'" + storeName + "' is not a store" );
			return;
		}
		Store store = createStore( selectedStore );

		System.out.println( ud.getID() );
		System.out.println( store );

		Session session = store.newSession();

		boolean verbose = true;
		Log log = LogFactory.getLog( PushCmd.class );
		
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		System.out.println();
		System.out.println( "WHAT: " + ud.getID() );
		System.out.println( "WHEN: " + session );
		System.out.println();
		
		Collection<ManagedDiskDescriptor> existing = store.enumerate();
		if( verbose )
			System.out.println( "Stored data: " + existing );
		
		List<ManagedDiskDescriptor> matching =
			new ArrayList<ManagedDiskDescriptor>();
		for( ManagedDiskDescriptor el : existing ) {
			if( el.getDiskID().equals( ud.getID() ) ) {
				matching.add( el );
			}
		}
		Collections.sort( matching, ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		System.out.println( "Matching managed disks:" );
		for( ManagedDiskDescriptor el : matching ) {
			System.out.println( " " + el.getSession() );
		}

		ManagedDiskDigest digest = null;
		UUID uuid = null;
		if( !matching.isEmpty() ) {
			ManagedDiskDescriptor recent = matching.get( matching.size()-1 );
			log.info( "Retrieving uuid for: "+ recent );
			uuid = store.uuid( recent );
			if( verbose )
				System.out.println( "UUID: " + uuid );
			log.info( "Requesting digest for: "+ recent );
			digest = store.digest( recent );
			if( digest == null ) {
				System.out.println( "No digest, continuing with full disk put" );
				log.warn( "No digest, continuing with full disk put" );
			} else {
				System.out.println( "Retrieved digest for " +
						  recent.getSession() + ": " +
						  digest.size() );
				log.info( "Retrieved digest for " +
						  recent.getSession() + ": " +
						  digest.size() );
			}
			
		}

		ManagedDisk md = null;
		boolean useFlatDisk = ud.size() < 1024L * 1024 * 1024;
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
		int progressMonitorUpdateIntervalSecs = 5;
		store.put( md, cb, progressMonitorUpdateIntervalSecs );
	}
}

// eof
