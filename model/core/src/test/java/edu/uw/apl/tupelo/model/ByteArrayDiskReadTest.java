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
 * Testing read operations of {@link ByteArrayDisk}.  We use standard Unix
 * operations like dd, md5sum for 'expected' values of read
 * operations.
 */

public class ByteArrayDiskReadTest extends junit.framework.TestCase {

	static final String ONEGIGZEROSMD5 =
		"cd573cfaace07e7949bc0c46028904ff";

	static final String EIGHTGIGZEROSMD5 =
		"b770351fadae5a96bbaf9702ed97d28d";

	public void test_1G() {
		long sz = 1024 * 1024 * 1024;
		byte[] contents = new byte[(int)sz];
		ByteArrayDisk bad = new ByteArrayDisk( contents );

		/*
		  Expected: dd if=/dev/zero bs=1M count=1K | md5sum
		  Reading from /dev/zero valid since the contents array above
		  is all zeros too...
		*/
		
		test( bad, contents.length, ONEGIGZEROSMD5 );
	}

	public void test_1G_SpeedLimited() {
		byte[] contents = new byte[1024 * 1024 * 1024];
		ByteArrayDisk bad = new ByteArrayDisk( contents );
		bad.setReadSpeed( 1 << 26 );
		/*
		  Expected: dd if=/dev/zero bs=1M count=1K | md5sum
		  Reading from /dev/zero valid since the contents array above
		  is all zeros too...
		*/
		
		test( bad, contents.length, ONEGIGZEROSMD5 );
	}

	public void test_8G() {
		long sz = 1024L * 1024 * 1024 * 8;
		byte[] contents = new byte[1<<30];
		ByteArrayDisk bad = new ByteArrayDisk( sz, contents );

		/*
		  Expected: dd if=/dev/zero bs=1M count=8K | md5sum
		  Reading from /dev/zero valid since the contents array above
		  is all zeros too...
		*/
		
		test( bad, sz, EIGHTGIGZEROSMD5 );
	}

	private void test( MemoryDisk md, long sz, String expectedMD5 ) {
		testRead2EOF( md, sz );
		testMD5Sum( md, expectedMD5 );
	}

	private void testMD5Sum( MemoryDisk md, String expected ) {
		String actual = null;
		try {
			InputStream is = md.getInputStream();
			actual = Utils.md5sum( is );
			is.close();
		} catch( IOException ioe ) {
			fail();
		}
		assertEquals( actual, expected );
	}
		
	private void testRead2EOF( MemoryDisk md, long expected ) {
		byte[] buf = new byte[1024*1024];
		long actual = 0;
		try {
			InputStream is = md.getInputStream();
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
