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

public class FlatDiskTest extends junit.framework.TestCase {

	public void testNull() {
	}

	// A sized file which should PASS the 'whole number of sectors' test
	public void testSize64k() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		} catch( IllegalArgumentException iae ) {
			fail();
		}
	}
	
	// A sized file which should FAIL the 'whole number of sectors' test
	public void testSize1000() throws IOException {
		File f = new File( "src/test/resources/1000" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			FlatDisk fd = new FlatDisk( ud, Session.CANNED );
			fail();
		} catch( IllegalArgumentException iae ) {
			System.out.println( "Expected: " + iae );
		}
	}


	public void testWriteCanned1() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		testWriteCanned( f );
	}

	public void testWriteCanned2() throws IOException {
		File f = new File( "src/test/resources/1m" );
		if( !f.exists() )
			return;
		testWriteCanned( f );
	}

	private void testWriteCanned( File f ) throws IOException {
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );

		File output = new File( f.getParent(),
								f.getName() + ManagedDisk.FILESUFFIX );
		System.out.println( output );
		fd.writeTo( output );
		assertEquals( f.length() + 512, output.length() );
	}

	public void testReadManagedCanned1() throws IOException {
		File raw = new File( "src/test/resources/64k" );
		if( !raw.exists() )
			return;
		testReadManagedCanned( raw );
	}

	public void testReadManagedCanned2() throws IOException {
		File raw = new File( "src/test/resources/1m" );
		if( !raw.exists() )
			return;
		testReadManagedCanned( raw );
	}

	private void testReadManagedCanned( File raw ) throws IOException {
		File managed = new File( raw.getPath() + ManagedDisk.FILESUFFIX );
		if( !managed.exists() )
			return;

		String md5_1 = Utils.md5sum( raw );
		
		ManagedDisk md = ManagedDisk.readFrom( managed );
		assertEquals( md.header.type, ManagedDisk.DiskTypes.FLAT );

		InputStream is = md.getInputStream();
		String md5_2 = Utils.md5sum( is );
		is.close();

		assertEquals( md5_1, md5_2 );
	}

}

// eof
