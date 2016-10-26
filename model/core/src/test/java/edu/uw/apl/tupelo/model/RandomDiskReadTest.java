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
import java.io.IOException;
import java.io.InputStream;

/**
 * Testing read operations of {@link RandomDisk}.  Since all data read
 * is 'random', the only tests we can apply are byte counts.  Byte
 * contents are essentialy untestable, what would 'expected values' be
 * ??
 */
public class RandomDiskReadTest extends junit.framework.TestCase {

	public void test_1G() {
		long sz = 1024L * 1024 * 1024;
		RandomDisk rd = new RandomDisk( sz, 1024 );

		test( rd, sz );
	}

	// A typical real disk size, 128GB
	public void _test_128G() {
		long sz = 1024L * 1024 * 1024 * 128;
		RandomDisk rd = new RandomDisk( sz, 1024 );

		/*
		  Expected: dd if=/dev/zero bs=1M count=128K | md5sum
		  Warning: this may take a while, took 5+ mins on rejewski
		*/
		test( rd, sz );
	}

	private void test( RandomDisk rd, long sz ) {
		testRead2EOF( rd, sz );
	}

	private void testRead2EOF( RandomDisk rd, long expected ) {
		byte[] buf = new byte[1024*1024*128];
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
}

// eof
