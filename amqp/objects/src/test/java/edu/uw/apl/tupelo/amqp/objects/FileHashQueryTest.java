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
package edu.uw.apl.tupelo.amqp.objects;

import java.lang.reflect.Type;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class FileHashQueryTest extends junit.framework.TestCase {

	protected void setUp() {
		GsonBuilder gb = new GsonBuilder();
		gb.disableHtmlEscaping();
		gb.serializeNulls();
		gb.registerTypeAdapter(byte[].class,
							   new JSONSerializers.MessageDigestSerializer() );
		gson = gb.create();
	}

	public void testNull() {
	}

	public void test1() {
		FileHashQuery q1 = new FileHashQuery( "sha1" );
		q1.add( new byte[20] );
		q1.add( new byte[20] );
		RPCObject<FileHashQuery> rpc1 = RPCObject.asRPCObject( q1 );
		String s = gson.toJson( rpc1 );
		System.out.println( "Request: " + s );

		Type fhqType = new TypeToken<RPCObject<FileHashQuery>>(){}.getType();
		//		System.err.println( fhqType );
		
		RPCObject<FileHashQuery> rpc2 = gson.fromJson( s, fhqType );
		FileHashQuery q2 = rpc2.appdata;
		//report( q2 );

		assertEquals( q1.algorithm, q2.algorithm );
		assertEquals( q1.hashes.size(), q2.hashes.size() );
	}

	private void report( FileHashQuery q ) {
		System.out.println( q.algorithm );
		System.out.println( q.hashes.size() );
	}
	
	Gson gson;
}

// eof