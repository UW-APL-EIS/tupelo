package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Testing read operations of {@link RandomDisk}.  Since all data read
 * is 'random', the only tests we can apply are byte counts.  Byte
 * contents are essentialy untestable, what would 'expected values' be
 * ??
 */
public class RandomDiskReadTest extends junit.framework.TestCase {

	public void test_1G() {
		long sz = 1024L * 1024 * 1024;
		RandomDisk rd = new RandomDisk( sz );

		test( rd, sz );
	}

	// A typical real disk size, 128GB
	public void _test_128G() {
		long sz = 1024L * 1024 * 1024 * 128;
		RandomDisk rd = new RandomDisk( sz );

		/*
		  Expected: dd if=/dev/zero bs=1M count=128K | md5sum
		  Warning: this may take a while, took 5+ mins on rejewski
		*/
		test( rd, sz );
	}

	private void test( RandomDisk rd, long sz ) {
		testRead2EOF( rd, sz );
	}

	private void testRead2EOF( RandomDisk rd, long expected ) {
		byte[] buf = new byte[1024*1024*128];
		long actual = 0;
		try {
			InputStream is = rd.getInputStream();
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
