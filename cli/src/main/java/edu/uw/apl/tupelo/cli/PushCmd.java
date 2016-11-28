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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;

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


public class PushCmd extends Command {

	PushCmd() {
		super( "push" );
		//, "Push local device content to a Tupelo store" );

		requiredArgs( "deviceName", "storeName" );
	}
	
	@Override
	public void invoke( Config config, boolean verbose,
						CommandLine cl ) throws Exception {

		String[] args = cl.getArgs();

		String deviceName = args[0];
		Config.Device selectedDevice = null;
		for( Config.Device d : config.devices() ) {
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

		if( false ) {
			System.out.println( ud.getID() );
			System.out.println( store );
		}
		Session session = store.newSession();

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
		
		List<ManagedDiskDescriptor> matching = new ArrayList<>();
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
				System.out.println
					( "No digest, continuing with full disk push" );
				log.warn( "No digest, continuing with full disk push" );
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
