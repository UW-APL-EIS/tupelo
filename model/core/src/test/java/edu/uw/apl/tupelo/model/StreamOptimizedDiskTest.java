package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class StreamOptimizedDiskTest extends junit.framework.TestCase {

	public void testNull() {
	}

	// A sized file which should PASS the 'whole number of grains' test
	public void testSize64k() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		} catch( IllegalArgumentException iae ) {
			fail();
		}
	}
	
	// A sized file which should FAIL the 'whole number of grains' test
	public void testSize1000() throws IOException {
		File f = new File( "src/test/resources/1000" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
			fail();
		} catch( IllegalArgumentException iae ) {
			System.out.println( "Expected: " + iae );
		}
	}

	// A sized file which should FAIL the 'whole number of grains' test
	public void testSize1k() throws IOException {
		File f = new File( "src/test/resources/1k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
			fail();
		} catch( IllegalArgumentException iae ) {
			System.out.println( "Expected: " + iae );
		}
	}

	public void _testManage32m() throws IOException {
		File f = new File( "src/test/resources/32m.zero" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		File out = new File( "/dev/null" );
		md.writeTo( out );

	}

	public void _testNuga2() throws IOException {
		File f = new File( "data/nuga2.dd" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		File out = new File( f.getName() + ManagedDisk.FILESUFFIX );
		md.writeTo( out );

	}

	public void testManagedNuga2() throws IOException {
		File f = new File( "nuga2.dd" + ManagedDisk.FILESUFFIX );
		if( !f.exists() )
			return;
		ManagedDisk md = ManagedDisk.readFrom( f );
	}
}

// eof
