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
package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

/**
 * @author Stuart Maclean
 *
 * Using FUSE, expose the named store's content under a local mount point.
 * Then, arbitrary system software can walk our stored disks, e.g. Sleuthkit.
 *
 * LOOK: This command called MDFS = ManagedDiskFileSystem.  Better to
 * re-name to MountCmd ??
 */

public class MDFSCmd extends Command {
	MDFSCmd() {
		super( "mount" );
		addAlias( "mount" );
		requiredArgs( "storeName", "dir" );
	}
	
	@Override
	public void invoke( Config config, boolean verbose,
						CommandLine cl )
		throws Exception {

		String[] args = cl.getArgs();
		String storeName = args[0];
		String mountPointName = args[1];

		File mountPoint = new File( mountPointName );
		if( !mountPoint.isDirectory() ) {
			System.err.println( "Mount point not a directory: " + mountPoint );
			return;
		}
		
		Config.Store selectedStore = null;
		for( Config.Store cs : config.stores() ) {
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

		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( fs );

		if( verbose )
			System.out.println( "Mounting '" + mountPoint + "'" );

		/*
		  We WANT to block, so do NOT want ownThread.  A separate
		  process will have to 'fusermount -u mountPoint'
		*/
		
		System.out.println( "To umount: 'fusermount -u " + mountPoint + "'" );
		boolean ownThread = false;
		mdfs.mount( mountPoint, ownThread );
	}
}

// eof
