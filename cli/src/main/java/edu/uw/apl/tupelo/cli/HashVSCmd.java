package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

import edu.uw.apl.commons.sleuthkit.image.Image;
import edu.uw.apl.commons.sleuthkit.volsys.VolumeSystem;
import edu.uw.apl.commons.sleuthkit.digests.VolumeSystemHash;
import edu.uw.apl.commons.sleuthkit.digests.VolumeSystemHashCodec;

public class HashVSCmd extends Command {
	HashVSCmd() {
		super( "hashvs", "Hash unallocated areas of a store-managed disk" );
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
			System.err.println( "Need store arg + index" );
			return;
		}
		Config c = new Config();
		c.load( config );
		
		String storeName = args[0];
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

		FilesystemStore fs = (FilesystemStore)store;

		int index = Integer.parseInt( args[1] );

		Collection<ManagedDiskDescriptor> mdds = null;
		try {
			mdds = store.enumerate();
		} catch( ConnectException ce ) {
			System.err.println
				( "Network Error. Is the remote Tupelo store up?" );
			System.exit(0);
		}
		if( index < 1 || index > mdds.size() ) {
			System.err.println( "Index out-of-range" );
			return;
		}

		final boolean debug = true;
		
		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted,
						  ManagedDiskDescriptor.DEFAULTCOMPARATOR );

		ManagedDiskDescriptor mdd = sorted.get(index-1);
		
		Log log = LogFactory.getLog( HashVSCmd.class );
		log.info( "Hashing " + mdd );

		String key = "hashvs";
		byte[] value = store.getAttribute( mdd, key );
		if( value != null )
			return;

		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( fs );
		
		final File mountPoint = new File( "mdfs-mount" );
		if( !mountPoint.exists() ) {
			mountPoint.mkdirs();
			mountPoint.deleteOnExit();
		}
		if( debug )
			System.out.println( "Mounting '" + mountPoint + "'" );
		mdfs.mount( mountPoint, true );
		Runtime.getRuntime().addShutdownHook( new Thread() {
				public void run() {
					if( debug )
						System.out.println( "Unmounting '" + mountPoint + "'" );
					try {
						mdfs.umount();
					} catch( Exception e ) {
						System.err.println( e );
					}
				}
			} );
		
		// LOOK: wait for the fuse mount to finish.  Grr hate arbitrary sleeps!
		Thread.sleep( 1000 * 2 );


		File f = mdfs.pathTo( mdd );
		Image i = new Image( f );
		try {
			VolumeSystem vs = null;
			try {
				vs = new VolumeSystem( i );
			} catch( IllegalStateException noVolSys ) {
				log.warn( noVolSys );
				return;
			}
			try {
				VolumeSystemHash vsh = VolumeSystemHash.create( vs );
				StringWriter sw = new StringWriter();
				VolumeSystemHashCodec.writeTo( vsh, sw );
				String s = sw.toString();
				value = s.getBytes();
				store.setAttribute( mdd, key, value );
			} finally {
				vs.close();
			}
		} finally {
			i.close();
		}
	}
}

// eof
