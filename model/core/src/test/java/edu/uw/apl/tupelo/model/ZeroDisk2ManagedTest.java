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
 * Testing conversion of {@link ZeroDisk}, an unmanaged disk
 * implementation, to {@link ManagedDisk} status.  Since we know the
 * exact contents of a ZeroDisk (all zeros), its only variable factor
 * is its size, which we impose.
 *
 * This is really more a test of ManagedDisks implementations given a known
 * UnmanagedDisk layout/content.
 *
 * @see FlatDisk
 * @see StreamOptimizedDisk
 */
public class ZeroDisk2ManagedTest extends junit.framework.TestCase {

	public void test_1G_Flat() throws IOException {
		long sz = 1024L * 1024 * 1024;
		ZeroDisk zd = new ZeroDisk( sz );
		zd.setReadSpeed( 1024L*1024*16 );

		FlatDisk fd = new FlatDisk( zd, Session.CANNED );
		File out = new File( "flat." + zd.getID() );
		fd.writeTo( out );
		assertEquals( sz + FlatDisk.Header.SIZEOF, out.length() );
		out.delete();
	}

	public void test_1G_StreamOptimized() throws IOException {
		long sz = 1024L * 1024 * 1024;
		ZeroDisk zd = new ZeroDisk( sz );
		zd.setReadSpeed( 1024L*1024*128 );

		StreamOptimizedDisk sod = new StreamOptimizedDisk( zd, Session.CANNED );
		File out = new File( "streamoptimized." + zd.getID() );
		sod.writeTo( out );

		/*
		  LOOK: Not much of a test.  Should be able to calculate the
		  exact byte count for a StreamoptimizedDisk built from an
		  all-zeros unmnanaged counterpart!
		*/
		assertTrue( sz > out.length() );

		out.delete();
	}
}

// eof
