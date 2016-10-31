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
import java.io.IOException;
import java.io.InputStream;

public class VerifyTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void test1gStreamOptimized() throws IOException {
		File f = new File( "./1g" );
		if( !f.exists() )
			return;
		System.out.println( "test1gStreamOptimized: " + f );
		UnmanagedDisk ud = new DiskImage( f );
		StreamOptimizedDisk sod = new StreamOptimizedDisk( ud, Session.CANNED );
		File mf = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( mf );
		sod.writeTo( fos );
		fos.close();
		sod.setManagedData( mf );
		try {
			sod.verify();
		} catch( IllegalStateException ise ) {
			fail();
		}
	}

	public void test1gFlat() throws IOException {
		File f = new File( "./1g" );
		if( !f.exists() )
			return;
		System.out.println( "test1gFlat: " + f );
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		File mf = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( mf );
		fd.writeTo( fos );
		fos.close();
		fd.setManagedData( mf );
		try {
			fd.verify();
		} catch( IllegalStateException ise ) {
			fail();
		}
	}
}

// eof
