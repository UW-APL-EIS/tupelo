package edu.uw.apl.tupelo.model;

import java.io.File;

/**
 * @author Stuart Maclean
 *
 * PhysicalDisk relies on DeviceFile for its low-level info access,
 * i.e disk serial number, size, etc.  Here we are testing how
 * PhysicalDisk can be used when the native load of DeviceFile fails,
 * normally due to the code being run on a platform for which
 * DeviceFile native was never built.
 
 * This test MUST be run on its own, via e.g.
 *
 * mvn test -Dtest=DeviceFileLoadFailTest
 *
 * and NOT with other tests.  All tests are apparently done in a
 * single VM, and by a single class loader (??).  So if a test run
 * before this one successfully loads the Device File native library,
 * we cannot later force a deliberate load failure.
 */

public class DeviceFileLoadFailTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testSDA() throws Exception {
		File f = new File( "/dev/sda" );
		if( !f.canRead() )
			return;

		// This forces the native code load in DeviceFile to be skipped
		System.setProperty( "edu.uw.apl.commons.devicefiles.disabled", "" );

		// A PhysicalDisk object can be built ok
		PhysicalDisk pd = new PhysicalDisk( f );

		// But size or id access should be defaults..
		long sz = pd.size();
		assertTrue( sz == 0 );

		String id = pd.getID();
		assertEquals( f.getPath(), id );
	}
}

// eof
