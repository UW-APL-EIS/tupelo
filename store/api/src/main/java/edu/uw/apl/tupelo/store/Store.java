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
package edu.uw.apl.tupelo.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.model.Session;

/**
 * @author Stuart Maclean
 *
 */

public interface Store {

	public UUID getUUID() throws IOException;
	
	public long getUsableSpace() throws IOException;
	
	public Session newSession() throws IOException;

	public void put( ManagedDisk md ) throws IOException;

	public void put( ManagedDisk md, ProgressMonitor.Callback cb,
					 int progressUpdateIntervalSecs ) throws IOException;

	/**
	 * @return size, in bytes, of the managed disk described by the
	 * supplied descriptor. Return -1 if the descriptor does not
	 * identify a managed disk held in this store.
	 */
	public long size( ManagedDiskDescriptor mdd ) throws IOException;

	/**
	 * @return The 'createUUID' held in the header of the ManagedDisk
	 * identified by the supplied ManagedDiskDescriptor, or null if
	 * the descriptor does not identify a managed disk held in this
	 * store.
	 */
	public UUID uuid( ManagedDiskDescriptor mdd ) throws IOException;

	/**
	 * @return The ManagedDiskDigest associated with the ManagedDisk
	 * identified by the supplied ManagedDiskDescriptor, or null if
	 * the descriptor does not identify a managed disk held in this
	 * store OR there is no digest computed for the identified
	 * ManagedDisk.
	 */
	public ManagedDiskDigest digest( ManagedDiskDescriptor mdd )
		throws IOException;
	
	/**
	 * @return The names (keys) of all the attributes associated with
	 * the ManagedDisk identified by the supplied
	 * ManagedDiskDescriptor.
	 */
	public Collection<String> listAttributes( ManagedDiskDescriptor mdd )
		throws IOException;

	public void setAttribute( ManagedDiskDescriptor mdd,
							  String key, byte[] value ) throws IOException;


	public byte[] getAttribute( ManagedDiskDescriptor mdd, String key )
		throws IOException;
	
	// for the benefit of the fuse-based ManagedDiskFileSystem
	public ManagedDisk locate( ManagedDiskDescriptor mdd );

	/**
	 * @return A ManagedDiskDescriptor for each ManagedDisk held
	 * in the store at time of enumerate.  Since no real/natural
	 * ordering of ManagedDisk exists, we return Collection in
	 * preference to some ordered collection like List
	 */
	public Collection<ManagedDiskDescriptor> enumerate() throws IOException;

	/**
	 * Use with caution, could LOSE data
	 */
	//	public void clear();
}

// eof
