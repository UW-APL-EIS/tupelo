package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Stuart Maclean
 *
 * Testing the 'disk read speed per unit time' logic of our MemoryDisk
 * implementations.  We create various faked disk sizes and expected
 * read speeds.  A TB disk read is sloooooow, even at a fast read speed like
 * 500MBs-1.
 *
 * @see MemoryDisk
 * @see ZeroDisk
 */
public class MemoryDiskReadTimesTest extends junit.framework.TestCase {

	static final long ONE_MBPERSEC = 1L << 20;
	static final long ONEHUNDRED_MBPERSEC =  128 * (1L << 20);
	static final long FIVEHUNDRED_MBPERSEC = 512 * (1L << 20);
	
	public void test_1M() {
		long sz = 1024L * 1024;
		ZeroDisk zd = new ZeroDisk( sz, ONE_MBPERSEC );
		test( zd, 1 );
	}

	public void test_16M() {
		long sz = 1024L * 1024 * 16;
		ZeroDisk zd = new ZeroDisk( sz, ONE_MBPERSEC );
		test( zd, 16 );
	}

	public void test_1G() {
		long sz = 1L << 30;
		ZeroDisk zd = new ZeroDisk( sz, ONEHUNDRED_MBPERSEC );
		test( zd, 1 << 3 );
	}

	public void test_1T() {
		long sz = (1L << 40);
		ZeroDisk zd = new ZeroDisk( sz, FIVEHUNDRED_MBPERSEC );
		test( zd, 1 << 11 );
	}

	private void test( ZeroDisk zd, int expectedElapsedSeconds ) {
		System.out.println( "Size: " + zd.size() );
		
		long start = System.currentTimeMillis();
		try {
			byte[] bs = new byte[1024*1024*16];
			InputStream is = zd.getInputStream();
			while( true ) {
				int nin = is.read( bs );
				if( nin < 0 )
					break;
			}
		} catch( IOException ioe ) {
			fail();
		}
		long end = System.currentTimeMillis();
		int actualElapsedSeconds = (int)((end-start) / 1000);
		System.out.println( "Expected: " + expectedElapsedSeconds );
		System.out.println( "Actual: " + actualElapsedSeconds );
		
		assertTrue( Math.abs( expectedElapsedSeconds - actualElapsedSeconds )
					< 2 );
	}
}

// eof
