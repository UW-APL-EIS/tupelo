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

/**
 * @author Stuart Maclean
 *
 * A fake 'disk' in which all contents are zeros.  Any read just
 * returns zeroes. A variation of {@link MemoryDisk}, since has no
 * disk-backed data at all.
 *
 * @see RandomDisk
 * @see MemoryDisk
 */

public class ZeroDisk extends MemoryDisk {

	/**
	 * @param readSpeedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations. 200 MBs-1 is reasonable.
	 */
	public ZeroDisk( long sizeBytes, long readSpeedBytesPerSecond ) {
		this( sizeBytes, readSpeedBytesPerSecond,
			  ZeroDisk.class.getSimpleName() + "-" + sizeBytes );
	}
	
	/**
	 * @param speedBytesPerSecond - how many bytes can be read per second
	 * from this fake disk.  Used to put realistic load on read
	 * operations.
	 */
	public ZeroDisk( long sizeBytes, long speedBytesPerSecond, String id ) {
		super( sizeBytes, speedBytesPerSecond, id );
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new ZeroInputStream();
	}
	
	class ZeroInputStream extends MemoryDiskInputStream {
		ZeroInputStream() {
			buffer = new byte[0];
		}
		
		/**
		 * @param len - NOT the len that the caller passed in to the
		 * original read, but a computed maximum byte count from
		 * {@link MemoryDisk#read(byte[],int,int)}
		 *
		 * We just need to fill b with len zeros, starting at offset
		 * off.  We could just loop over len, just a System.arraycopy
		 * is likely faster. But that implies we HAVE a local buffer
		 * to copy FROM.  We guard against that buffer needing to be
		 * too huge, say 2GB!
		 */
		@Override
		protected int readImpl( byte[] b, int off, int len )
			throws IOException {
				
			if( len > buffer.length && len < MAXBUFFERSIZE ) {
				resize( len );
			}
			populate( b, off, len );
			return len;
		}

		private void resize( int len ) {
			buffer = new byte[len];
		}

		private void populate( byte[] b, int off, int len ) {
			int total = 0;
			int remaining = len - total;
			while( remaining >= buffer.length ) {
				System.arraycopy( buffer, 0, b, off+total, buffer.length );
				total += buffer.length;
				remaining -= buffer.length;
			}
			System.arraycopy( buffer, 0, b, off+total, remaining );
		}

		private byte[] buffer;

		static private final int MAXBUFFERSIZE = 1 << 20;
	}
}

// eof
