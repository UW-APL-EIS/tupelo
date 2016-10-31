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
// Note the extra underscore, since null is a reserved word in Java
package edu.uw.apl.tupelo.store.null_;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.TimeZone;

import org.apache.commons.io.output.NullOutputStream;

import edu.uw.apl.tupelo.model.Constants;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.store.Store;

/**
 * A Store implementation modeled on Unix file descriptor '/dev/null'.
 * Reads from it return 'empty/eof' and writes are no-ops.  For a
 * Store, it has no contents (data,attributes) and accepts neither.
 *
 * Useful as a test aid to see how long large (100GB+) ManagedDisk.put
 * operations will take, with the need for any extra store disk space
 * to accommodate the write.
 *
 * LOOK: should we return 'empty' structures, or nulls ??
 */

public class NullStore implements Store {

	@Override
	public String toString() {
		return "/dev/null";
	}
	
	@Override
	public UUID getUUID() throws IOException {
		return Constants.NULLUUID;
	}

	@Override
	public long getUsableSpace() throws IOException {
		return 0L;
	}
	
	@Override
	public Session newSession() throws IOException {
		return NULLSESSION;
	}

	@Override
	public long size( ManagedDiskDescriptor mdd ) {
		return -1;
	}

	@Override
	public UUID uuid( ManagedDiskDescriptor mdd ) {
		return null;
	}
	
	@Override
	public void put( ManagedDisk md ) throws IOException {
		/*
		  We do NOT just want to be an empty implementation, since
		  that would not stress the reading of the data from the
		  associated unmanaged disk.
		*/
		NullOutputStream nos = new NullOutputStream();
		md.writeTo( nos );
		nos.close();
	}

	@Override
	public synchronized void put( ManagedDisk md, ProgressMonitor.Callback cb,
								  int progressUpdateIntervalSecs )
		throws IOException {

		/*
		  We do NOT just want to be an empty implementation.
		  Rather, we want keep the progress monitor informed
		  so use a /dev/null like OutputStream
		*/
		NullOutputStream nos = new NullOutputStream();
		ProgressMonitor pm = new ProgressMonitor( md, nos, cb,
												  progressUpdateIntervalSecs );
		pm.start();
	}

	@Override
	public ManagedDiskDigest digest( ManagedDiskDescriptor mdd )
		throws IOException {

		// LOOK: should this be null ???
		return null;
	}
	
	@Override
	public Collection<String> listAttributes( ManagedDiskDescriptor mdd )
		throws IOException {

		// LOOK: should this be null ???
		return Collections.emptyList();
	}

	@Override
	public void setAttribute( ManagedDiskDescriptor mdd,
							  String key, byte[] value ) throws IOException {
	}

	@Override
	public byte[] getAttribute( ManagedDiskDescriptor mdd, String key )
		throws IOException {
		
		// LOOK: should this be null ???
		return new byte[0];
	}
	
	// for the benefit of the fuse-based ManagedDiskFileSystem
	@Override
	public ManagedDisk locate( ManagedDiskDescriptor mdd ) {
		return null;
	}
	
	@Override
	public Collection<ManagedDiskDescriptor> enumerate() throws IOException {

		// LOOK: should this be null ???
		return Collections.emptyList();
	}

	static private final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
	static private final Calendar EPOCH = Calendar.getInstance( UTC );
	static {
		EPOCH.setTimeInMillis( 0L );
	}
	
	static public final Session NULLSESSION = new Session
		( Constants.NULLUUID, EPOCH, 0 );
}

// eof
