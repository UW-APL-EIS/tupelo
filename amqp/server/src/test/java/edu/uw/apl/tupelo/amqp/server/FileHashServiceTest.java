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
package edu.uw.apl.tupelo.amqp.server;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.null_.NullStore;
import edu.uw.apl.tupelo.utils.Discovery;

public class FileHashServiceTest extends junit.framework.TestCase {

	public void testNull() {
	}

	/**
	 * Start a FileHashService in a background thread T.  Then stop
	 * that service from the main thread and join on T.  Testing that
	 * we can indeed stop/cancel a FileHashService cleanly once it is
	 * in operation.  We will need this teardown ability once we put a
	 * FileHashService component into a web-based store.

	 * LOOK: we are not asserting anything!
	 */
	public void test1() throws Exception {
		Store s = new NullStore();
		String url = Discovery.locatePropertyValue( "amqp.url" );
		final FileHashService fhs = new FileHashService( s, url );
		Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						fhs.start();
					} catch( Exception e ) {
						e.printStackTrace();
					}
				}
			};
		Thread t = new Thread( r );
		t.start();

		Thread.sleep( 4 * 1000L );
		fhs.stop();
		t.join();
	}
}

// eof
