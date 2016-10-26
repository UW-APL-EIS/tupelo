/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
