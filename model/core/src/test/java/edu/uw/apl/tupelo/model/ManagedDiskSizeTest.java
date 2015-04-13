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
