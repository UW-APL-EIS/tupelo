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

import java.io.Console;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

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

public class DigestCmd extends Command {
	DigestCmd() {
		super( "digest" );//, "Compute md5 hash for store-managed disks" );

		requiredArgs( "storeName" );
		optionalArg( "index" );
	}
	
	@Override
	public void invoke( Config config, boolean verbose,
						CommandLine cl )
		throws Exception {

		String[] args = cl.getArgs();
		String storeName = args[0];
		int index = -1;
		if( args.length > 1 ) {
			try {
				index = Integer.parseInt( args[1] );
			} catch( NumberFormatException nfe ) {
			}
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
		Store s = createStore( selectedStore );
		if( !( s instanceof FilesystemStore ) ) {
			System.err.println( "Can only digest on a filesystemStore" );
			return;
		}
		FilesystemStore fs = (FilesystemStore)s;
		Collection<ManagedDiskDescriptor> mdds = fs.enumerate();
		List<ManagedDiskDescriptor> sorted = new ArrayList( mdds );
		Collections.sort( sorted,
						  ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		if( index > -1 ) {
			if( index < 1 || index > sorted.size() ) {
				System.err.println( "Selected index out-of-bounds: " + index );
				return;
			}
			ManagedDiskDescriptor mdd = sorted.get(index-1);
			long sz = fs.size( mdd );
			System.out.println( "Digesting: " + mdd +
								" (" + sz + " bytes)" );
			fs.computeDigest( mdd );
		} else {
			for( ManagedDiskDescriptor mdd : sorted ) {
				boolean proceed = proceedTest( mdd );
				if( !proceed )
					continue;
				long sz = fs.size( mdd );
				System.out.println( "Digesting: " + mdd +
									" (" + sz + " bytes)" );
				fs.computeDigest( mdd );
			}
		}
	}

	private boolean proceedTest( ManagedDiskDescriptor mdd ) {
		Console c = System.console();
		if( c == null )
			return true;
		String line = c.readLine( "" + mdd + "? y/n " );
		if( line == null )
			return false;
		line = line.trim();
		if( line.isEmpty() )
			return false;
		return line.charAt(0) == 'y' || line.charAt(0) == 'Y';
	}
}

// eof
