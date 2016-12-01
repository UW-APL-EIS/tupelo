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
package edu.uw.apl.tupelo.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * @author Stuart Maclean
 *
 * A fake 'disk' in which contents come from a byte[].  We maintain
 * the byte[] by reference, enabling the user to mutate the data, just
 * like a real disk contents would mutate over time.  Like all MemoryDisks,
 * has no actual I/O operations at all.
 *
 *
 * Useful in testing Tupelo store puts, especially with parent links
 * and the use of parent digests.  We build a single ByteArrayDisk,
 * but mutate its content between Store.put operations:
 *
 * byte[] contents = new byte[N];
 * ByteArrayDisk bad = new ByteArrayDisk( contents );
 * ... store operations ...
 *
 * Mutate the unmanaged 'disk';
 * contents[X] = 18; contents[Y] = 21; ....
 *
 * ... more store operations ...
 *
 * @see MemoryDisk
 */

public class ByteArrayDisk extends MemoryDisk {

	public ByteArrayDisk( byte[] data ) {
		this( data, 0 );
	}
	
	/**
	 * @param readSpeedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations. 200 MBs-1 is reasonable.
	 */
	public ByteArrayDisk( byte[] data, long readSpeedBytesPerSecond ) {
		super( data.length, readSpeedBytesPerSecond );
		this.data = data;
	}
	
	@Override
	protected InputStream inputStreamImpl() throws IOException {
		return new ByteArrayInputStream( data );
	}

	private final byte[] data;
}

// eof
