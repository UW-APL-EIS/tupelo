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
