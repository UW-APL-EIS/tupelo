package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.input.CountingInputStream;

/**
 * Testing aspects of reading data from a PhysicalDisk. Warning, these
 * tests can read a LOT of data, 100GB+, so can take tens of minutes.
 * We really want a Maven profile (in additon to tester) that says
 * 'Yes I really want to run these lengthy (read slow!) tests
 */
public class ReadTest extends junit.framework.TestCase {

	public void testNull() {
	}

	/**
	   Testing that we really can read as many bytes as the disk's
	   advertised size. Note that 250GB took 40+ mins to read, on
	   rejewski.
	   
	   Running edu.uw.apl.tupelo.model.ReadTest
	   Expected: 250000000000
	   Actual: 250000000000

	   [INFO] ---------------------------------------------------------
	   [INFO] BUILD SUCCESS
	   [INFO] ---------------------------------------------------------
	   [INFO] Total time: 41:16.609s
	   [INFO] Finished at: Thu Nov 13 12:54:53 PST 2014
	   [INFO] Final Memory: 14M/170M
	   [INFO] ---------------------------------------------------------
	*/
	public void _testAdvertisedSize() throws Exception {
		File f = new File( "/dev/sda" );
		if( !f.canRead() )
			return;
		PhysicalDisk pd = new PhysicalDisk( f );
		long expected = pd.size();
		System.out.println( "Expected: " + expected );
		byte[] ba = new byte[1024*1024];
		InputStream is = pd.getInputStream();
		CountingInputStream cis = new CountingInputStream( is ); 
		int nin = 0;
		while( (nin = cis.read( ba )) != -1 )
			;
		cis.close();
		long actual = cis.getByteCount();
		System.out.println( "Actual: " + actual );
		assertEquals( expected, actual );
	}
}

// eof
