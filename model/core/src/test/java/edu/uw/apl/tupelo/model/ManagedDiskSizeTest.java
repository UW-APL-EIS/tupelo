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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Testing the on-disk size of all {@link ManagedDisk} implementations.
 * The on-disk size of ManagedDisks should always be a whole sector count.
 *
 * @see ManagedDisk#readFromWriteTo( java.io.InputStream, java.io.OutputStream)
 */

public class ManagedDiskSizeTest extends junit.framework.TestCase {

	public void testSize64kStreamOptimized() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
			File out = new File( f.getName() + ".sd" );
			FileOutputStream fos = new FileOutputStream( out );
			md.writeTo( fos );
			assertTrue( out.length() % Constants.SECTORLENGTH == 0 );
		} catch( IllegalArgumentException iae ) {
			fail();
		}
	}

	public void testSize64kFlat() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new FlatDisk( ud, Session.CANNED );
			File out = new File( f.getName() + ".fd" );
			FileOutputStream fos = new FileOutputStream( out );
			md.writeTo( fos );
			assertTrue( out.length() % Constants.SECTORLENGTH == 0 );
		} catch( IllegalArgumentException iae ) {
			fail();
		}
	}
}

// eof
