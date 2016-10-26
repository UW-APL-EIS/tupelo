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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * @author Stuart Maclean
 *
 * An implementation of {@link UnmanagedDisk} in which all reads are
 * from local memory.  Has no backing disk at all.  Useful only in
 * testing/demonstration of course.
 *
 * @see RandomDisk
 * @see ZeroDisk
 */
 
abstract public class MemoryDisk implements UnmanagedDisk {

	/**
	 * @param readSpeedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations. 200 MBs-1 is reasonable.
	 */
	protected MemoryDisk( long sizeBytes, long readSpeedBytesPerSecond ) {
		this( sizeBytes, readSpeedBytesPerSecond,
			  MemoryDisk.class.getSimpleName() + "-" + sizeBytes );
	}

	/**
	 * @param readSeedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations.
	 */
	protected MemoryDisk( long sizeBytes, long readSpeedBytesPerSecond,
						  String id ) {
		this.size = sizeBytes;
		this.speedBytesPerSecond = readSpeedBytesPerSecond;
		this.id = id;
	}

	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public long size() {
		return size;
	}

	@Override
	public File getSource() {
		return new File( id );
	}
	
	abstract class MemoryDiskInputStream extends InputStream {
		MemoryDiskInputStream() {
			posn = 0;
		}
		
		abstract protected int readImpl( byte[] b, int off, int len )
			throws IOException;

		@Override
		public int available() throws IOException {
			// Cannot simply cast 'size - posn' to int, could get -ve value!
			long l = size - posn;
			if( l >= Integer.MAX_VALUE )
				return Integer.MAX_VALUE;
			return (int)l;
		}

		@Override
		public int read() throws IOException {
			byte[] ba = new byte[1];
			int n = read( ba, 0, 1 );
			if( n == -1 )
				return -1;
			return ba[0] & 0xff;
		}
		
		@Override
		public int read( byte[] b, int off, int len ) throws IOException {
			
			// checks from the contract for InputStream...
			if( b == null )
				throw new NullPointerException();
			if( off < 0 || len < 0 || off + len > b.length ) {
				throw new IndexOutOfBoundsException();
			}
			if( len == 0 )
				return 0;

			if( posn >= size )
				return -1;

			
			// Do min in long space, since size - posn may overflow int...
			long actualL = Math.min( size - posn, len );
			
			/*
			  Safe to cast to int, since know len and thus min is at most
			  maxInt, and cannot be -ve since checked above.
			*/
			int actual = (int)actualL;

			double delaySecs = actual / (double)speedBytesPerSecond;
			try {
				Thread.sleep( (long)(delaySecs * 1000) );
			} catch( InterruptedException ie ) {
			}
				
			int n = readImpl( b, off, actual );
			if( n == -1 ) {
				throw new IOException();
			}
			posn += n;
			return n;
		}

		@Override
		public long skip( long n ) throws IOException {
			if( n < 0 )
				return 0;
			long min = Math.min( n, size-posn );
			posn += min;
			return min;
		}

		private long posn;
	}

	protected final long size;
	protected final long speedBytesPerSecond;
	protected final String id;
}

// eof
