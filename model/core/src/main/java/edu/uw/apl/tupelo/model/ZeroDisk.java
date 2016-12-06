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

import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.io.input.NullInputStream;

/**
 * @author Stuart Maclean
 *
 * A fake 'disk' in which all contents are zeros.  Any read just
 * returns zeroes. A variation of {@link MemoryDisk}, since has no
 * disk-backed data at all.
 *
 * Uses commons.io's NullInputStream, a nice class which already takes
 * care of the moving 'position' (file pointer) in the data as the
 * user calls read (and perhaps skip)
 *
 * @see ByteArrayDisk
 * @see RandomDisk
 * @see MemoryDisk
 */

public class ZeroDisk extends MemoryDisk {

	public ZeroDisk( long sizeBytes ) {
		super( sizeBytes );
	}
	
	/**
	 * @param readSpeedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations. 200 MBs-1 is reasonable.
	 */
	public ZeroDisk( long sizeBytes, long readSpeedBytesPerSecond ) {
		super( sizeBytes, readSpeedBytesPerSecond );
	}
	
	@Override
	protected InputStream inputStreamImpl() throws IOException {
		return new ZeroInputStream();
	}
	
	class ZeroInputStream extends NullInputStream {
		ZeroInputStream() {
			super( size, false, false );
		}

	   	@Override
		protected int processByte() {
			/*
			  The read has already been done, and position moved
			  along by 1
			*/
			long indx = getPosition() - 1;
			int m = mutatedValue( indx );
			return m > -1 ? m : 0;
		}
		
		@Override
		protected void processBytes( byte[] ba, int offset, int length ) {
			/*
			  The read has already been done, and position moved
			  along by length bytes
			*/
			long lo = getPosition() - length;
			for( int i = 0; i < length; i++ ) {
				int m = mutatedValue( lo+i );
				ba[offset+i] = (m > -1) ? (byte)m : 0;
			}
		}
	}
}

// eof
