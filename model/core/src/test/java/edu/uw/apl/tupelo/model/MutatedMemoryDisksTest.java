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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Stuart Maclean
 *
 * Testing read operations of various {@link MemoryDisk} implementations.
 * Specifically, we are testing the 'set' operation and its
 * way of mutating the contents of the 'disk' content over time.
 *
 * See also the cmdr.sh shell script for reference file creation.
 */
public class MutatedMemoryDisksTest extends junit.framework.TestCase {

	private byte[] loadMutFile( File f ) throws IOException {
		byte[] result = new byte[(int)f.length()];
		RandomAccessFile raf = new RandomAccessFile( f, "r" );
		raf.readFully( result );
		raf.close();
		return result;
	}

	// MMD1 is 1GB of zeros mutated by 1 MB of ones at offset 0
	public void _test_MMD1() throws Exception {
		byte[] ba = loadMutFile( new File( "ones_20" ) );

		long sz = 1024L * 1024 * 1024;
		MemoryDisk md = new ZeroDisk( sz );
		md.set( 0, ba );
		
		// expected: md5sum mmd1.dat
		test( md, sz, "c836f772d30ec3fbaa3ff436eafdeee3" );
	}

	/*
	  MMD2 is 1GB of randoms with seed=21, then mutated by 1 MB of
	  ones at offset 8MB
	*/
	public void test_MMD2() throws Exception {
		byte[] ba = loadMutFile( new File( "ones_20" ) );

		long sz = 1024L * 1024 * 1024;
		MemoryDisk md = new RandomDisk( sz, 0, 21 );
		md.set( 8 * (1 << 20), ba );
		
		// expected: md5sum mmd2.dat
		test( md, sz, "c03f485b449346b5957756214c9b34b8" );
	}

	/*
	  MMD3 is 1GB of randoms with seed=21, then mutated by 1 MB of
	  ones at offset 8MB and then further mutated by 16MB of zeros at
	  offset 32M
	*/
	public void test_MMD3() throws Exception {
		byte[] ones = loadMutFile( new File( "ones_20" ) );
		byte[] zeros = new byte[1<<24];
		long sz = 1024L * 1024 * 1024;
		
		MemoryDisk md = new RandomDisk( sz, 0, 21 );
		md.set( 8  * (1 << 20), ones );
		md.set( 32 * (1 << 20), zeros );
		
		// expected: md5sum mmd3.dat
		test( md, sz, "d7b033a70d76e2baee5ebf6de0475e7d" );
	}

	/*
	*/
	public void _test_ByteArrayDisk_1G_1() {
		int sz = 1024 * 1024 * 1024;
		byte[] ba = new byte[sz];
		ByteArrayDisk bad = new ByteArrayDisk( sz, ba );

		/*
		  A 'null' mutation, writing more zeros into a
		  region of an already all-zeros disk
		*/
		int ONEMEG = 1 << 20;
		byte[] mut = new byte[ONEMEG];
		bad.set( 512 * ONEMEG, mut );
		
		// expected: dd if=/dev/zero bs=1M count=1K | md5sum
		test( bad, sz, "cd573cfaace07e7949bc0c46028904ff" );
	}
	
	private void test( MemoryDisk md, long sz, String expectedMD5 ) {
		//testRead2EOF( zd, sz );
		testMD5Sum( md, expectedMD5 );
	}

	private void testMD5Sum( MemoryDisk md, String expected ) {
		String actual = null;
		try {
			InputStream is = md.getInputStream();
			actual = Utils.md5sum( is, _64M );
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

	static final int _64M = 1 << 26;
}

// eof
