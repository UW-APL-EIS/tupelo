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
import java.util.Random;

import org.apache.commons.io.input.NullInputStream;

/**
 * @author Stuart Maclean
 *
 * A test/fake implementation of an UnmanagedDisk.  This one is an
 * in-memory disk (needs no real backing disk) which produces random
 * data when read.  The 'random' nature does NOT give a new random
 * buffer for each read, it just re-uses the same buffer of say 16MB
 * over and over.  The 'randomness' of the data is then confined to that
 * one local buffer, which is 're-sampled' over and over.
 *
 * By default, the random stream produced is seeded by the Disk size, so
 * RandomDisks of same size produce identical content.  Such disks
 * cannot 'change content' over time.  Can also supply a seed.
 *
 * Uses commons.io's NullInputStream, a nice class which already takes
 * care of the moving 'position' (file pointer) in the data as the
 * user calls read (and perhaps skip)
 */
public class RandomDisk extends MemoryDisk {

	public RandomDisk( long size ) {
		this( size, 0, size );
	}

	/**
	 * @param size byte count for this disk
	 *
	 * @param speedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations. Can be 0.
	 */
	public RandomDisk( long size, long readSpeedBytesPerSecond ) {
		this( size, readSpeedBytesPerSecond, size );
	}

	public RandomDisk( long size, long readSpeedBytesPerSecond, long seed ) {
		super( size, readSpeedBytesPerSecond );
		this.seed = seed;
	}
	
	@Override
	protected InputStream inputStreamImpl() throws IOException {
		return new RandomDiskInputStream();
	}

	class RandomDiskInputStream extends NullInputStream {
		RandomDiskInputStream() {
			
			super( size, false, false );
			Random rng = new Random( seed );
			buffer = new byte[BUFFERLENGTH];
			rng.nextBytes( buffer );
		}

	   	@Override
		protected int processByte() {
			/*
			  The read has already been done, and position moved
			  along by 1
			*/
			long indx = getPosition() - 1;
			return buffer[(int)(indx % buffer.length)] & 0xff;
		}
		
		@Override
		protected void processBytes( byte[] ba, int offset, int length ) {
			if( false )
				return;
			
			/*
			  The read has already been done, and position moved
			  along by length bytes
			*/
			long lo = getPosition() - length;
			for( int i = 0; i < length; i++ ) {
				int indx = (int)((lo + i) % buffer.length);
				ba[offset+i] = buffer[indx];
			}
		}
		
		private final byte[] buffer;

	}

	private final long seed;

	static final int BUFFERLENGTH = 1 << 24;
}

// eof
