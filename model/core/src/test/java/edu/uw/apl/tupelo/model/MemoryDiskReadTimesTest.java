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
 * @author Stuart Maclean
 *
 * Testing the 'disk read speed per unit time' logic of our MemoryDisk
 * implementations.  We create various faked disk sizes and expected
 * read speeds.  A TB disk read is sloooooow, even at a fast read speed like
 * 500MBs-1.
 *
 * @see MemoryDisk
 * @see ZeroDisk
 */
public class MemoryDiskReadTimesTest extends junit.framework.TestCase {

	static final long ONE_MBPERSEC = 1L << 20;
	static final long ONEHUNDRED_MBPERSEC =  128 * (1L << 20);
	static final long FIVEHUNDRED_MBPERSEC = 512 * (1L << 20);
	
	public void test_1M() {
		long sz = 1024L * 1024;
		ZeroDisk zd = new ZeroDisk( sz );
		zd.setReadSpeed( ONE_MBPERSEC );
		test( zd, 1 );
	}

	public void test_16M() {
		long sz = 1024L * 1024 * 16;
		ZeroDisk zd = new ZeroDisk( sz );
		zd.setReadSpeed( ONE_MBPERSEC );
		test( zd, 16 );
	}

	public void test_1G() {
		long sz = 1L << 30;
		ZeroDisk zd = new ZeroDisk( sz );
		zd.setReadSpeed( ONEHUNDRED_MBPERSEC );
		test( zd, 1 << 3 );
	}

	public void test_1T() {
		long sz = (1L << 40);
		ZeroDisk zd = new ZeroDisk( sz );
		zd.setReadSpeed( FIVEHUNDRED_MBPERSEC );
		test( zd, 1 << 11 );
	}

	private void test( ZeroDisk zd, int expectedElapsedSeconds ) {
		System.out.println( "Size: " + zd.size() );
		
		long start = System.currentTimeMillis();
		try {
			byte[] bs = new byte[1024*1024*16];
			InputStream is = zd.getInputStream();
			while( true ) {
				int nin = is.read( bs );
				if( nin < 0 )
					break;
			}
		} catch( IOException ioe ) {
			fail();
		}
		long end = System.currentTimeMillis();
		int actualElapsedSeconds = (int)((end-start) / 1000);
		System.out.println( "Expected: " + expectedElapsedSeconds );
		System.out.println( "Actual: " + actualElapsedSeconds );
		
		assertTrue( Math.abs( expectedElapsedSeconds - actualElapsedSeconds )
					< 2 );
	}
}

// eof
