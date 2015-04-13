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

		FlatDisk fd = new FlatDisk( zd, Session.CANNED );
		File out = new File( "flat." + zd.getID() );
		fd.writeTo( out );
		assertEquals( sz + FlatDisk.Header.SIZEOF, out.length() );
		out.delete();
	}

	public void test_1G_StreamOptimized() throws IOException {
		long sz = 1024L * 1024 * 1024;
		ZeroDisk zd = new ZeroDisk( sz );

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
