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
package edu.uw.apl.tupelo.store.filesys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.model.ZeroDisk;

/**
 * @author Stuart Maclean
 *
 * Using known ManagedDisk implementations, specifically ZeroDisks,
 * and pushing instances to a filesys Store, we can assert exact
 * file size on disk of the Managed .tmd files.
 */

public class SizeTest extends junit.framework.TestCase {

	static final File ROOT = new File( "store-sizetest" );
	
	FilesystemStore store;
	
	protected void setUp() {
		ROOT.mkdirs();
		try {
			store = new FilesystemStore( ROOT );
		} catch( IOException ioe ) {
			System.err.println( ioe );
			fail();
		}
	}

	protected void tearDown() {
		try {
			FileUtils.deleteDirectory( ROOT );
		} catch( IOException ioe ) {
			System.err.println( ioe );
			fail();
		}
	}

	public void testSizeZero10() throws Exception {
		ZeroDisk z10 = new ZeroDisk( 1 << 10, 1 << 20 );
		testSize( z10, 1 << 10 );
	}

	public void testSizeZero20() throws Exception {
		ZeroDisk z20 = new ZeroDisk( 1 << 20, 1 << 20 );
		testSize( z20, 1 << 20 );
	}

	public void testSizeZero30() throws Exception {
		ZeroDisk z30 = new ZeroDisk( 1 << 30, 100 * (1 << 20) );
		testSize( z30, 1 << 30 );
	}

	/**
	 * Test that a put + size of a known UnmanagedDisk equals
	 * the known size
	 */
	private void testSize( UnmanagedDisk ud, long expectedSize )
		throws Exception {
		Session session = store.newSession();

		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		System.out.println( mdd );

		FlatDisk fd = new FlatDisk( ud, session );
		store.put( fd );
		long szfd = store.size( mdd );
		assertEquals( szfd, expectedSize );

		session = store.newSession();
		StreamOptimizedDisk sod = new StreamOptimizedDisk( ud, session );
		store.put( sod );
		ManagedDiskDescriptor mdd2 = new ManagedDiskDescriptor( ud.getID(),
															   session );
		long szsod = store.size( mdd2 );
		assertEquals( szsod, expectedSize );
	}
}

// eof
