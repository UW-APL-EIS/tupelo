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
package edu.uw.apl.tupelo.http.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class PathsTest extends junit.framework.TestCase {

	public void testDiskID() {
		String did = "32m";

		Pattern p = ManagedDiskDescriptor.DISKIDREGEX;
		Matcher m = p.matcher( did );
		assert( m.matches() );
		
	}

	public void testSession() {
		Session s = Session.CANNED;
		String f = "" + s;

		Pattern p = Session.SHORTREGEX;
		Matcher m = p.matcher( f );
		assert( m.matches() );
	}

	public void testPathInfo1() {
		String pi = "32m/12345678.1234";
		Pattern p = HttpStoreServlet.MDDPIREGEX;
		Matcher m = p.matcher( pi );
		assert( m.matches() );
	}

	public void testPathInfo2() {
		String pi = "32m/20141023.0019";
		Pattern p = HttpStoreServlet.MDDPIREGEX;
		Matcher m = p.matcher( pi );
		assert( m.matches() );
	}
	
}

// eof
