package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Testing read operations of {@link ZeroDisk}.  We use standard Unix
 * operations like dd, md5sum for 'expected' values of read
 * operations.
 */
public class ZeroDiskReadTest extends junit.framework.TestCase {

	public void test_1G() {
		long sz = 1024L * 1024 * 1024;
		ZeroDisk zd = new ZeroDisk( sz );

		// expected: dd if=/dev/zero bs=1M count=1K | md5sum
		test( zd, sz, "cd573cfaace07e7949bc0c46028904ff" );
	}

	// A typical real disk size, 128GB
	public void test_128G() {
		long sz = 1024L * 1024 * 1024 * 128;
		ZeroDisk zd = new ZeroDisk( sz );

		/*
		  Expected: dd if=/dev/zero bs=1M count=128K | md5sum
		  Warning: this may take a while, took 5+ mins on rejewski
		*/
		test( zd, sz, "35a06e21f6bbb512aedac9671904ffd8" );
	}

	private void test( ZeroDisk zd, long sz, String expectedMD5 ) {
		testRead2EOF( zd, sz );
		testMD5Sum( zd, expectedMD5 );
	}

	private void testMD5Sum( ZeroDisk zd, String expected ) {
		String actual = null;
		try {
			InputStream is = zd.getInputStream();
			actual = Utils.md5sum( is );
			is.close();
		} catch( IOException ioe ) {
			fail();
		}
		assertEquals( actual, expected );
	}
		
	private void testRead2EOF( ZeroDisk zd, long expected ) {
		byte[] buf = new byte[1024*1024];
		long actual = 0;
		try {
			InputStream is = zd.getInputStream();
			while( true ) {
				int nin = is.read( buf, 0, buf.length );
				if( nin == -1 )
					break;
				actual += nin;
			}
			is.close();
		} catch( IOException ioe ) {
			fail();
		}
		assertEquals( actual, expected );
	}
}

// eof
