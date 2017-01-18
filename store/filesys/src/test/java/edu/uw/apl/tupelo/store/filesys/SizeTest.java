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
import edu.uw.apl.tupelo.model.ManagedDisk;
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
 *
 * Note how the use of setUp and tearDown ensure a completely
 * clean/empty store for each test, done by literally wiping away
 * all files and directories under the store ROOT.
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
		ZeroDisk z10 = new ZeroDisk( 1 << 10 );
		testSizeFlat( z10, 1 << 10 );
		testSizeStreamOptimized( z10, 1 << 10 );
	}

	public void testSizeZero20() throws Exception {
		ZeroDisk z20 = new ZeroDisk( 1 << 20 );
		testSizeFlat( z20, 1 << 20 );
		testSizeStreamOptimized( z20, 1 << 20 );
	}

	public void testSizeZero30() throws Exception {
		ZeroDisk z30 = new ZeroDisk( 1 << 30 );
		z30.setReadSpeed( 100 * (1 << 20) );
		testSizeFlat( z30, 1 << 30 );
		testSizeStreamOptimized( z30, 1 << 30 );
	}

	/**
	 * Test that a put + size of a known UnmanagedDisk stored as a
	 * FlatDisk equals the known/expected size
	 */
	private void testSizeFlat( UnmanagedDisk ud,
							   long expectedDataSize )
		throws Exception {
		Session session = store.newSession();

		FlatDisk fd = new FlatDisk( ud, session );
		store.put( fd );

		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		long szfd = store.size( mdd );
		assertEquals( szfd, expectedDataSize );

		/*
		  For FlatDisks, the on-disk file is simply all
		  the logical disk data prefixed by a Header, of known/fixed
		  size
		*/
		File onDisk = store.managedDataFile( ROOT, mdd );
		System.out.println( onDisk + " " + onDisk.length() );
		assertTrue( onDisk.length() == expectedDataSize +
					ManagedDisk.Header.SIZEOF );
	}

	/**
	 * Test that a put + size of a known UnmanagedDisk stored as a
	 * StreamOptimizedDiskDisk equals the known/expected size
	 */
	private void testSizeStreamOptimized( UnmanagedDisk ud,
										  long expectedDataSize )
		throws Exception {
		
		Session session = store.newSession();
		StreamOptimizedDisk sod = new StreamOptimizedDisk( ud, session );
		store.put( sod );
		
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
																session );
		long szsod = store.size( mdd );
		assertEquals( szsod, expectedDataSize );

		sod.report( System.out );
	}
}

// eof
