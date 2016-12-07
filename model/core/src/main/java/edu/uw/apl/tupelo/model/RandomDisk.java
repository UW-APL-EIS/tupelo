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

	/**
	 * When the entire disk content is handled by a single extent.
	 * The disk size also doubles up as the random stream seed.
	 *
	 * @param size - the overall disk size
	 */
	public RandomDisk( long size ) {
		this( size, (int)size, size );
	}
	/**
	 * @param size - the overall disk size
	 * @param extent - the length of the maintained rnd byte stream.
	 * Extents are then chained together to make up the required
	 * disk size.  Thus the random stream repeats over and over.
	 */
	public RandomDisk( long size, int extent ) {
		this( size, extent, size );
	}

	public RandomDisk( long size, int extent, long seed ) {
		super( size );
		if( size % extent != 0 ) {
			throw new IllegalArgumentException
				( "Extent length (" + extent + ")" +
				  " must divide size (" + size + ")" );
		}
		Random rng = new Random( seed );
		buffer = new byte[extent];
		rng.nextBytes( buffer );
	}
	
	@Override
	protected byte supplyByte( long offset ) {
		/*
		  buffer.length MUST be 2^n, we are optimising the A % B
		  operation as A & (B-1), which requires B is 2^N.
		*/
		int indx = (int)(offset & (buffer.length-1));
		return buffer[indx];
	}

	private final byte[] buffer;
}

// eof
