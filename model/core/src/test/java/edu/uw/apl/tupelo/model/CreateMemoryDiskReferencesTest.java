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
vv * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Random;

/**
 * @author Stuart Maclean
 *
 * For various MemoryDisk tests, see e.g. MutatedMemoryDisksTest, we
 * need some reference files from which we compute MD5 hashes.  Here we
 * build those files, assuming sufficient disk space is available.
 *
 * To build the data required:
 *
 * Step1 : run this test case.
 * Step2 : ./cmdr.sh
 *
 * @see MemoryDisk
 * @see MutatedMemoryDisksTest
 
 */
public class CreateMemoryDiskReferencesTest extends junit.framework.TestCase {

	private void createFixedContentFile( int len, int value, File f )
		throws IOException {

		if( f.exists() )
			return;
		byte[] bs = new byte[len];
		for( int i = 0; i < bs.length; i++ )
			bs[i] = (byte)value;
		FileOutputStream os = new FileOutputStream( f );
		os.write( bs );
		os.close();
	}

	private void createRandomContentFile( long size, int extent,
										  long seed, File f )
		throws IOException {

		if( f.exists() )
			return;

		if( size % extent != 0 )
			throw new IllegalArgumentException
				( "size (" + size + ") must be multiple of " +
				  "extent (" + extent + ")" );

		byte[] bs = new byte[extent];
		Random r = new Random( seed );
		r.nextBytes( bs );

		int chunks = (int)(size / extent);
		FileOutputStream fos = new FileOutputStream( f );
		BufferedOutputStream bos = new BufferedOutputStream( fos,
															 1 << 20 );
		for( int i = 0; i < chunks; i++ ) {
			bos.write( bs );
		}
		bos.flush();
		bos.close();
		fos.close();
	}

	public void testCreateFiles()
		throws Exception {

		// One million '1's...
		File f1 = new File( "ones_20" );
		System.out.println( f1 );
		createFixedContentFile( 1 << 20, 1, f1 );

		// One billion randoms...
		File f2 = new File( "rnd21_30" );
		System.out.println( f2 );
		createRandomContentFile( 1 << 30, 1 << 30, 21, f2 );
	}
}
// eof
