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
