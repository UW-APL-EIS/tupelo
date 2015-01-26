package edu.uw.apl.tupelo.store.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;

/**
 * A container for state common to all command line-driven tools in
 * this module which require traversal of a fuse-based
 * ManagedDiskFileSystem.
 *
 * @see BodyFile
 * @see HashFS
 * @see HashVS
 * 
 */

abstract class MDFSBase extends Base {

	protected MDFSBase() {
		super();
	}

		/**
	 * @param mdfsPath - the filename as exported by the ManagedDiskFileSystem
	 * which represents the managed disk.
	 *
	 * @param mdd - the managed disk descriptor (diskID,session), used for
	 * composing any output results file name.
	 */
	abstract protected void process( File mdfsPath,
									 ManagedDiskDescriptor mdd )
		throws Exception;



	protected void start() throws Exception {
		File dir = new File( storeLocation );
		if( !dir.isDirectory() ) {
			throw new IllegalStateException
				( "Not a directory: " + storeLocation );
		}
		store = new FilesystemStore( dir );
		if( debug )
			System.out.println( "Store type: " + store );

		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		
		final File mountPoint = new File( "test-mount" );
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
		
		if( all ) {
			Collection<ManagedDiskDescriptor> mdds = store.enumerate();
			List<ManagedDiskDescriptor> sorted =
				new ArrayList<ManagedDiskDescriptor>( mdds );
			Collections.sort( sorted,
							  ManagedDiskDescriptor.DEFAULTCOMPARATOR );
 			for( ManagedDiskDescriptor mdd : sorted ) {
				File f = mdfs.pathTo( mdd );
				System.out.println( "Processing: " + mdd );
				process( f, mdd );
			}
		} else {
			ManagedDiskDescriptor mdd = Utils.locateDescriptor
				( store, diskID, sessionID );
			if( mdd == null ) {
				System.err.println( "Not stored: " + diskID + "," +
									sessionID );
				System.exit(1);
			}
			File f = mdfs.pathTo( mdd );
			process( f, mdd );
		}
	}
}

// eof
