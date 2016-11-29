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
 * Unit tests based around FilesystemStore.put
 */

public class PutTest extends junit.framework.TestCase {

	static final File ROOT = new File( "store-puttest" );
	
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

	public void _testSizeZero10() throws Exception {
		ZeroDisk zd = new ZeroDisk( 1 << 10, 1 << 20 );
		testDuplicatePut( zd );
	}

	public void testSizeZero20() throws Exception {
		ZeroDisk z20 = new ZeroDisk( 1 << 20, 1 << 20 );
		testDuplicatePut( z20 );
		testPutN( z20 );
	}

	/*
	  Put the same UnmanagedDisk 2+ times into a store,
	  as would be done in practice for repeated acquisitions
	  over time of the same drive
	*/
	private void testPutN( UnmanagedDisk ud )
		throws Exception {

		Collection<ManagedDiskDescriptor> mdds0 = store.enumerate();
				   
		Session session1 = store.newSession();
		FlatDisk fd1 = new FlatDisk( ud, session1 );
		store.put( fd1 );
		Collection<ManagedDiskDescriptor> mdds1 = store.enumerate();
		assertTrue( mdds1.size() == (mdds0.size() + 1) );

		Session session2 = store.newSession();
		FlatDisk fd2 = new FlatDisk( ud, session2 );
		store.put( fd2 );
		Collection<ManagedDiskDescriptor> mdds2 = store.enumerate();

		assertTrue( mdds2.size() == (mdds1.size() + 1) );

	}

	private void testDuplicatePut( UnmanagedDisk ud )
		throws Exception {

		Collection<ManagedDiskDescriptor> mdds1 = store.enumerate();
		assertTrue( mdds1.isEmpty() );
				   
		Session session = store.newSession();

		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		System.out.println( mdd );

		FlatDisk fd = new FlatDisk( ud, session );
		store.put( fd );
		
		Collection<ManagedDiskDescriptor> mdds2 = store.enumerate();
		assertTrue( mdds2.size() == 1 );

		// A duplicate put attempt...
		FlatDisk fd2 = new FlatDisk( ud, session );
		try {
			store.put( fd2 );
			fail();
		} catch( RuntimeException re ) {
		}
		Collection<ManagedDiskDescriptor> mdds3 = store.enumerate();
		assertTrue( mdds3.size() == mdds2.size() );
	}
}

// eof
