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

import org.apache.commons.codec.binary.Hex;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class FileHashResponseTest extends junit.framework.TestCase {

	protected void setUp() {
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(Session.class,
							   new JSONSerializers.SessionSerializer() );
		gb.registerTypeAdapter(byte[].class,
							   new JSONSerializers.MessageDigestSerializer() );
		gb.disableHtmlEscaping();
		gb.serializeNulls();
		gson = gb.create();
	}

	public void testNull() {
	}

	public void test1() throws Exception {
		FileHashResponse r1 = new FileHashResponse( "md5" );
		String diskID = "seagate1234";
		Session session = Session.testSession();
		String hashHex = "1234567890123456789012345678901234567890";
		byte[] hash = Hex.decodeHex( hashHex.toCharArray() );
		r1.add( hash, new ManagedDiskDescriptor( diskID, session ),
			   "/" );

		String r1json = gson.toJson( r1 );
		System.out.println( "R1: " + r1json );
		
		RPCObject<FileHashResponse> rpc1 = RPCObject.asRPCObject( r1 );
		String s = gson.toJson( rpc1 );
		System.out.println( "RPC1: " + s );

		Type fhrType = new TypeToken<RPCObject<FileHashResponse>>(){}.getType();
		RPCObject<FileHashResponse> rpc2 = gson.fromJson( s, fhrType );
		FileHashResponse r2 = rpc2.appdata;

		// LOOK: what are we asserting ??
		System.out.println( "R2: " + r2.paramString() );
	}

	Gson gson;
}

// eof
