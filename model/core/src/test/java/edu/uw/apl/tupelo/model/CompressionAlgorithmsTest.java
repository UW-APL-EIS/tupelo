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
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CompressionAlgorithmsTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testWriteNuga2() throws IOException {
		File f = new File( "data/nuga2.dd" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );

		testWrite( md, ManagedDisk.Compressions.DEFLATE );
		testWrite( md, ManagedDisk.Compressions.GZIP );
		testWrite( md, ManagedDisk.Compressions.SNAPPY );
	}

	private void testWrite( ManagedDisk md, ManagedDisk.Compressions c )
		throws IOException {

		long start = System.currentTimeMillis();
		md.setCompression( c );
		ManagedDiskDescriptor mdd = md.getDescriptor();
		File out = new File( mdd.getDiskID() + ManagedDisk.FILESUFFIX +
							 "-compress." + c.ordinal() );
		
		FileOutputStream fos = new FileOutputStream( out );
		BufferedOutputStream bos = new BufferedOutputStream( fos, 1024*64 );
		md.writeTo( bos );
		bos.close();
		fos.close();
		long stop = System.currentTimeMillis();

		System.out.println( out + " -> " + (stop-start)/1000 );
	}

	public void testReadNuga2() throws IOException {

		ManagedDisk.Compressions[] cs = {
			ManagedDisk.Compressions.DEFLATE,
			ManagedDisk.Compressions.GZIP,
			ManagedDisk.Compressions.SNAPPY,
		};
		for( ManagedDisk.Compressions c : cs ) {
			File f = new File( "nuga2.dd" + ManagedDisk.FILESUFFIX +
							   "-compress." + c.ordinal() );
			System.out.println( f );
			if( !f.exists() )
				return;
			ManagedDisk md = ManagedDisk.readFrom( f );
			InputStream is = md.getInputStream();
			long start = System.currentTimeMillis();
			String md5 = Utils.md5sum( is );
			is.close();
			long stop = System.currentTimeMillis();
			System.out.println( md5 );
			
			System.out.println( f + " -> " + (stop-start)/1000 );
		}
	}
}

// eof
