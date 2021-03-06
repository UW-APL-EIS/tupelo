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
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.NullInputStream;

/**
 * @author Stuart Maclean
 *
 * An implementation of {@link UnmanagedDisk} in which all reads are
 * from local memory.  Has no backing disk at all.  Useful only in
 * testing/demonstration of course.
 *
 * Is passed a 'read speed' at construction time, for the purposes of
 * realistic read speeds, comparable to disk I/O.  If this read speed,
 * we add NO delays to reads.
 *
 * @see UnmanagedDisk
 * @see ByteArrayDisk
 * @see RandomDisk
 * @see ZeroDisk
 */
 
abstract public class MemoryDisk implements UnmanagedDisk {

	abstract protected byte supplyByte( long offset );

	protected MemoryDisk( long sizeBytes ) {
		this.size = sizeBytes;
		mutations = new ArrayList<>();
	}
	
	/**
	 * @param readSpeedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations. 200 MBs-1 is reasonable.  If 0,
	 * no limit is imposed on read speed.
	 */
	public void setReadSpeed( long readSpeedBytesPerSecond ) {
		this.readSpeedBytesPerSecond = readSpeedBytesPerSecond;
	}

	@Override
	public String getID() {
		return getClass().getSimpleName() + "-" + size;
	}
	
	@Override
	public long size() {
		return size;
	}

	@Override
	public File getSource() {
		// Is NOT expected to be a File which exists!
		return new File( getID() );
	}

	/*
	  How to mutate a MemoryDisk, supply a byte range with offset.
	  Many mutations are supported
	*/
	public void set( long offset, byte[] bs ) {
		if( offset + bs.length > size )
			throw new IllegalArgumentException( getID() +
												": region extends past size: " +
												offset+ bs.length );
		mutations.add( new Region( offset, bs ) );
	}

	private int mutatedValue( long offset ) {
		for( Region r : mutations ) {
			if( offset >= r.offset &&
				offset < r.offset + r.data.length ) {
				return r.data[(int)(offset - r.offset)] & 0xff;
			}
		}
		return -1;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream impl = new WithMutationsNullInputStream();
		if( readSpeedBytesPerSecond > 0 )
			return new SpeedLimitedInputStream( impl );
		// If NOT limiting read speed, simply give the caller the impl...
		return impl;
	}

	static class Region {
		Region( long offset, byte[] bs ) {
			this.offset = offset;
			this.data = bs;
		}
		final long offset;
		final byte[] data;
	}
	
	/*
	  SpeedLimitedInputStream imposes the readSpeed limit on read thru-put.
	  It's a FilterInputStream where the 'filtering' is simply the delay
	  after a read.  The actual data comes from whatever Inputstream
	  is passed to its constructor.
	*/
	class SpeedLimitedInputStream extends FilterInputStream {
		SpeedLimitedInputStream( InputStream is ) {
			super( is );
		}

		@Override
		public int read( byte[] b, int off, int len ) throws IOException {
			int n = super.read( b, off, len );
			if( n > 0 ) {
				double delaySecs = n / (double)readSpeedBytesPerSecond;
				try {
					Thread.sleep( (long)(delaySecs * 1000) );
				} catch( InterruptedException ie ) {
				}
			}
			return n;
		}
	}

	class WithMutationsNullInputStream extends NullInputStream {
		protected WithMutationsNullInputStream() {
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
			return m > -1 ? m : supplyByte( indx );
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
				ba[offset+i] = (m > -1) ? (byte)m : supplyByte( lo+i );
			}
		}
	}

	private final long size;
	private long readSpeedBytesPerSecond;
	private final List<Region> mutations;
}

// eof
