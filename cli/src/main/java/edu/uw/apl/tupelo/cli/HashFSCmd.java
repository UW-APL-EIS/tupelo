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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.codec.binary.Hex;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.store.tools.HashFS;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class HashFSCmd extends Command {
	HashFSCmd() {
		// "Hash file content of a store-managed disk" );
		super( "hashfs" );

		option( "p",
				"Print existing hashfs result(s) for the identified disk.");
		requiredArgs( "storeName", "index" );
	}

	@Override
	public void invoke( Config config, boolean verbose,
						CommandLine cl )
		throws Exception {

		boolean print = cl.hasOption( "p" );
		String[] args = cl.getArgs();
		String storeName = args[0];
		int index = -1;
		try {
			index = Integer.parseInt( args[1] );
		} catch( NumberFormatException nfe ) {
			HelpCmd.INSTANCE.commandHelp( this );
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

		Collection<ManagedDiskDescriptor> mdds = null;
		try {
			mdds = store.enumerate();
		} catch( ConnectException ce ) {
			System.err.println
				( "Network Error. Is the remote Tupelo store up?" );
			return;
		}
		if( index < 1 || index > mdds.size() ) {
			System.err.println( "Index out-of-range" );
			return;
		}
		
		List<ManagedDiskDescriptor> sorted = new ArrayList( mdds );
		Collections.sort( sorted,
						  ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		ManagedDiskDescriptor mdd = sorted.get(index-1);

		if( print ) {
			report( mdd, store );
		} else {
			process( mdd, store, storeName );
		}
	}

	static void process( ManagedDiskDescriptor mdd, Store store,
						 String storeName )
		throws Exception {
		
		if( !( store instanceof FilesystemStore ) ) {
			System.err.println( "Not a FileSystemStore: " + storeName );
			return;
		}
		FilesystemStore fs = (FilesystemStore)store;

		Log log = LogFactory.getLog( HashFSCmd.class );
		log.info( "Hashing " + mdd );
		
		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( fs );
		
		final File mountPoint = new File( "mount-hashfs" );
		if( !mountPoint.exists() ) {
			mountPoint.mkdirs();
			mountPoint.deleteOnExit();
		}
		if( true )
			System.out.println( "Mounting '" + mountPoint + "'" );
		mdfs.mount( mountPoint, true );
		Runtime.getRuntime().addShutdownHook( new Thread() {
				public void run() {
					if( true )
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
		HashFS.process( f, mdd, fs );
	}

	static void report( ManagedDiskDescriptor mdd, Store store )
		throws Exception {

		Collection<String> attrNames = store.listAttributes( mdd );
		for( String name : attrNames ) {
			if( name.startsWith( "hashfs" ) ) {
				String key = name;
				byte[] value = store.getAttribute( mdd, key );
				if( value != null ) {
					String s = new String( value );
					System.out.println( s );
				}
			}
		}
	}
	
	static private final Log log = LogFactory.getLog( HashFSCmd.class );

}

// eof
