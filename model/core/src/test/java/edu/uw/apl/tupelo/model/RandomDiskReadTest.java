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
import java.io.SequenceInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * Testing read operations of {@link RandomDisk}.  Since all data read
 * is 'random', the only tests we can apply are byte counts.  Byte
 * contents are essentialy untestable, what would 'expected values' be
 * ??
 */
public class RandomDiskReadTest extends junit.framework.TestCase {

	static String md5RndBytes( long size, long seed )
		throws IOException {
		if( size > 1 << 30 )
			return md5RndBytesSequenced( size, 1 << 20, seed );
		Random r = new Random( seed );
		byte[] ba = new byte[(int)size];
		r.nextBytes( ba ); 
		ByteArrayInputStream bais = new ByteArrayInputStream( ba );
		String md5 = Utils.md5sum( bais );
		bais.close();
		return md5;
	}

	static String md5RndBytesSequenced( long size, int extent,
										long seed )
		throws IOException {

		int chunks = (int)(size / extent );
		
		Random r = new Random( seed );
		byte[] ba = new byte[extent];
		r.nextBytes( ba );
		Vector<InputStream> iss = new Vector<>( chunks );
		for( int i = 1; i <= chunks; i++ ) {
			//			System.err.println( "md5RndBytesSequenced " + i );
			iss.add( new ByteArrayInputStream( ba ) );
		}
		Enumeration<InputStream> e = iss.elements();
		SequenceInputStream sis = new SequenceInputStream( e );
		String md5 = Utils.md5sum( sis, _64M );
		sis.close();
		return md5;
	}
	
	public void test_1M() throws IOException {
		long sz = 1024L * 1024;
		RandomDisk rd = new RandomDisk( sz, (int)sz );
		String md5 = md5RndBytes( sz, sz );
		test( rd, sz, md5 );
	}

	public void test_16M() throws IOException {
		long sz = 1024L * 1024 * 16;
		RandomDisk rd = new RandomDisk( sz );
		String md5 = md5RndBytes( sz, sz );
		test( rd, sz, md5 );
	}

	public void test_64M() throws IOException {
		long sz = 1024L * 1024 * 64;
		RandomDisk rd = new RandomDisk( sz );
		String md5 = md5RndBytes( sz, sz );
		test( rd, sz, md5 );
	}

	public void test_2GB() throws IOException {
		long sz = 1024L * 1024 * 1024 * 2;
		RandomDisk rd = new RandomDisk( sz, 1<<20, sz );
		String md5 = md5RndBytesSequenced( sz, 1<<20, sz );
		test( rd, sz, md5 );
	}

	public void test_32GB() throws IOException {
		long sz = 1024L * 1024 * 1024 * 32;
		RandomDisk rd = new RandomDisk( sz, 1 << 26, sz );
		String md5 = md5RndBytesSequenced( sz, 1 << 26, sz );
		test( rd, sz, md5 );
	}

	// A typical real disk size, 128GB
	public void test_128G() throws IOException {
		long sz = 1024L * 1024 * 1024 * 128;
		RandomDisk rd = new RandomDisk( sz, 1 << 28, sz );
		String md5 = md5RndBytesSequenced( sz, 1 << 28, sz );

		/*
		  Expected: dd if=/dev/zero bs=1M count=128K | md5sum
		  Warning: this may take a while, took 5+ mins on rejewski
		*/
		test( rd, sz, md5 );
	}

	private void test( RandomDisk rd, long sz, String md5Expected ) {
		//testRead2EOF( rd, sz );
		testMD5Sum( rd, md5Expected );
	}

	private void testRead2EOF( RandomDisk rd, long expected ) {
		byte[] buf = new byte[_128M];
		long actual = 0;
		try {
			InputStream is = rd.getInputStream();
			while( true ) {
				int nin = is.read( buf, 0, buf.length );
				if( nin == -1 )
					break;
				actual += nin;
			}
			is.close();
		} catch( IOException ioe ) {
			fail();
		}
		assertEquals( actual, expected );
	}

	private void testMD5Sum( RandomDisk rd, String expected ) {
		String actual = null;
		try {
			InputStream is = rd.getInputStream();
			actual = Utils.md5sum( is, _64M );
			is.close();
		} catch( IOException ioe ) {
			fail();
		}
		assertEquals( actual, expected );
	}

	static final int _16M = 1 << 24;

	static final int _64M = 1 << 26;

	static final int _128M = 1 << 27;
}

// eof
