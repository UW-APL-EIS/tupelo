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
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.model.Utils;
import edu.uw.apl.tupelo.model.ZeroDisk;

/**
 * @author Stuart Maclean
 *
 * Unit tests based around the FilesystemStore.put operation.
 *
 * Note how the use of setUp and tearDown ensure a completely
 * clean/empty store for each test, done by literally wiping away
 * all files and directories under the store ROOT.
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

	public void testSizeZero10() throws Exception {
		ZeroDisk zd = new ZeroDisk( 1 << 10, 1 << 20 );
		tests( zd );
	}

	public void testSizeZero20() throws Exception {
		ZeroDisk zd = new ZeroDisk( 1 << 20, 1 << 20 );
		tests( zd );
	}

	public void testSizeZero24() throws Exception {
		ZeroDisk zd = new ZeroDisk( 1 << 24, 1 << 20 );
		tests( zd );
	}

	private void tests( UnmanagedDisk ud ) throws Exception {
		testDuplicatePut( ud );
		testPutNFlat( ud );
		testPutNCompressed( ud );
		//		testPutNCompressedWithDigest( ud );
		testContentRoundTrip( ud );
	}
	
	/*
	  Put the same UnmanagedDisk 2+ times into a store, as would be
	  done in practice for repeated acquisitions over time of the same
	  drive.  Use FlatDisks. Each put has its own Session.
	*/
	private void testPutNFlat( UnmanagedDisk ud )
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
		compareStoredDisks( fd1.getDescriptor(), fd2.getDescriptor() );
	}

	/*
	  Put the same UnmanagedDisk 2+ times into a store, as would be
	  done in practice for repeated acquisitions over time of the same
	  drive.  Use StreamOptimizedDisks, all compression types.  Each
	  put has its own Session. Assert various properties of the
	  Store and the newly added ManagedDisks.
	*/
	private void testPutNCompressed( UnmanagedDisk ud )
		throws Exception {

		for( ManagedDisk.Compressions c : ManagedDisk.Compressions.values() ) {
			Session session1 = store.newSession();
			StreamOptimizedDisk md1 = new StreamOptimizedDisk( ud, session1 );
			md1.setCompression( c );
			store.put( md1 );
			Collection<ManagedDiskDescriptor> sz1 = store.enumerate();
			ManagedDiskDescriptor mdd1 = md1.getDescriptor();
			UUID uuid = store.uuid( mdd1 );
			
			Session session2 = store.newSession();
			StreamOptimizedDisk md2 = new StreamOptimizedDisk
				( ud, session2, uuid );
			md2.setCompression( c );
			store.put( md2 );
			Collection<ManagedDiskDescriptor> sz2 = store.enumerate();

			assertTrue( sz2.size() == (sz1.size() + 1) );

			compareStoredDisks( mdd1, md2.getDescriptor() );
		}
	}

	/*
	  Put the same UnmanagedDisk 2+ times into a store, as would be
	  done in practice for repeated acquisitions over time of the same
	  drive.  Use StreamOptimizedDisks. Each put has its own Session.
	*/
	private void testPutNCompressedWithDigest( UnmanagedDisk ud )
		throws Exception {

		for( ManagedDisk.Compressions c : ManagedDisk.Compressions.values() ) {
			Session session1 = store.newSession();
			StreamOptimizedDisk md1 = new StreamOptimizedDisk( ud, session1 );
			md1.setCompression( c );
			store.put( md1 );
			Collection<ManagedDiskDescriptor> sz1 = store.enumerate();

			ManagedDiskDescriptor mdd1 = md1.getDescriptor();
			store.computeDigest( mdd1 );
			ManagedDiskDigest dig = store.digest( mdd1 );
			assertNotNull( dig );
			UUID uuid = store.uuid( mdd1 );
			assertNotNull( uuid );
			
			Session session2 = store.newSession();
			StreamOptimizedDisk md2 = new StreamOptimizedDisk( ud, session2,
															   uuid );
			md2.setCompression( c );
			md2.setParentDigest( dig );
			store.put( md2 );
			Collection<ManagedDiskDescriptor> sz2 = store.enumerate();

			assertTrue( sz2.size() == (sz1.size() + 1) );
			compareStoredDisksWithDigest( mdd1, md2.getDescriptor() );
		}
	}
	
	/*
	  Assert that ManagedDisks stored in a FilesystemStore which
	  represent the same logical Unmanaged data result in stored files
	  of equal length.  Would be true for all FlatDisk stores and also
	  any StreamOptimizedDisk stores NOT taking advantage of parent
	  links / digests.
	*/
	private void compareStoredDisks( ManagedDiskDescriptor mdd1,
									 ManagedDiskDescriptor mdd2 ) {
		File md1 = store.managedDataFile( ROOT, mdd1 );
		File md2 = store.managedDataFile( ROOT, mdd2 );

		System.out.println( md1 + " " + md1.length() );
		System.out.println( md2 + " " + md2.length() );

		assertEquals( md1.length(), md2.length() );
	}

	/*
	  mdd2 represents a stored ManagedDisk which was stored
	  using a known parent disk digest.  So store disk
	  for mdd2 should be smaller than that of mdd1, due to.
	  entire grain sequence have content 'same as parent'.

	  Assumed that the UnmanagedDisk data did not change
	  across the two puts, which holds since we are using ZeroDisks
	  for these tests
	*/
	
	private void compareStoredDisksWithDigest( ManagedDiskDescriptor mdd1,
											   ManagedDiskDescriptor mdd2 ) {
		File md1 = store.managedDataFile( ROOT, mdd1 );
		File md2 = store.managedDataFile( ROOT, mdd2 );

		System.out.println( md1 + " " + md1.length() );
		System.out.println( md2 + " " + md2.length() );
		
		assertTrue( md1.length() > md2.length() );
	}
	
	/*
	  Attempts to store the same data with the SAME sessionID more
	  than once should result in an exception thrown by the Store.
	*/
	private void testDuplicatePut( UnmanagedDisk ud )
		throws Exception {

		Session session = store.newSession();

		FlatDisk fd = new FlatDisk( ud, session );
		store.put( fd );
		Collection<ManagedDiskDescriptor> mdds1 = store.enumerate();
		
		// A duplicate put attempt...
		FlatDisk fd2 = new FlatDisk( ud, session );
		try {
			store.put( fd2 );
			fail();
		} catch( RuntimeException re ) {
		}
		Collection<ManagedDiskDescriptor> mdds2 = store.enumerate();
		assertTrue( mdds2.size() == mdds1.size() );
	}

	private void testContentRoundTrip( UnmanagedDisk ud )
		throws Exception {

		// Test data stored as FlatDisk is correctly round-tripped
		Session session = store.newSession();
		ManagedDisk md1 = new FlatDisk( ud, session );
		store.put( md1 );
		ManagedDiskDescriptor mdd = md1.getDescriptor();
		ManagedDisk md1Stored = store.locate( mdd );
		compareContent( ud, md1Stored );

		// Test data stored as StreamOptimizedDisk is correctly round-tripped
		for( ManagedDisk.Compressions c : ManagedDisk.Compressions.values() ) {
			session = store.newSession();
			ManagedDisk md2 = new StreamOptimizedDisk( ud, session );
			md2.setCompression( c );
			store.put( md2 );
			ManagedDiskDescriptor mdd2 = md2.getDescriptor();
			ManagedDisk md2Stored = store.locate( mdd2 );
			compareContent( ud, md2Stored );
		}
	}

	/*
	  Assert that the logical disk content as held in an UnmanagedDisk
	  is the same as that as held in the ManagedDisk counterpart.
	  Use md5 hash to assert byte[] equality.
	*/
	private void compareContent( UnmanagedDisk ud, ManagedDisk md )
		throws Exception {

		InputStream isu = ud.getInputStream();
		InputStream ism = md.getInputStream();

		String hu = Utils.md5sum( isu );
		String hm = Utils.md5sum( ism );
		assertEquals( hu, hm );
		
		ism.close();
		isu.close();
	}
	

}

// eof
