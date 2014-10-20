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
		try {
			ManagedDisk md = new StreamOptimizedDisk
				( f, "diskid", Session.CANNED );
		} catch( IllegalArgumentException iae ) {
			fail();
		}
	}
	
	// A sized file which should FAIL the 'whole number of grains' test
	public void testSize1000() throws IOException {
		File f = new File( "src/test/resources/1000" );
		if( !f.exists() )
			return;
		try {
			ManagedDisk md = new StreamOptimizedDisk
				( f, "diskid", Session.CANNED );
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
		try {
			ManagedDisk md = new StreamOptimizedDisk
				( f, "diskid", Session.CANNED );
			fail();
		} catch( IllegalArgumentException iae ) {
			System.out.println( "Expected: " + iae );
		}
	}
}

// eof
