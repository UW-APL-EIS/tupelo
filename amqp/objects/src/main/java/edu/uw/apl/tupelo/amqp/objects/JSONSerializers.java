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

import java.text.ParseException;

import com.google.gson.*;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class JSONSerializers {
	
	// For Json output of Session objects, we just use their .format method...
	static public class SessionSerializer
		implements JsonSerializer<Session>, JsonDeserializer<Session> {

		@Override
		public JsonElement serialize( Session src,
									  java.lang.reflect.Type typeOfSrc,
									  JsonSerializationContext context) {
			return new JsonPrimitive( src.format() );
		}

		@Override
		public Session deserialize( JsonElement json,
									java.lang.reflect.Type typeOfSrc,
									JsonDeserializationContext context)
			throws JsonParseException {
			String s = json.getAsJsonPrimitive().getAsString();
			try {
				Session result = Session.parse( s );
				return result;
			} catch( ParseException pe ) {
				throw new RuntimeException( pe );
			}
		}
	}

	/*
	  For Json output of MessageDigests hashes (byte[]), we use hex
	  encoding...
	*/
	static public class MessageDigestSerializer
		implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

		@Override
		public JsonElement serialize( byte[] src,
									  java.lang.reflect.Type typeOfSrc,
									  JsonSerializationContext context) {
			char[] cs = Hex.encodeHex( src );
			String s = new String( cs );
			return new JsonPrimitive( s );
		}

		@Override
		public byte[] deserialize( JsonElement json,
								   java.lang.reflect.Type typeOfSrc,
								   JsonDeserializationContext context)
			throws JsonParseException {
			String s = json.getAsJsonPrimitive().getAsString();
			char[] cs = s.toCharArray();
			try {
				byte[] ba = Hex.decodeHex( cs );
				return ba;
			} catch( DecoderException de ) {
				throw new RuntimeException( de );
			}
		}
	}
}

// eof

