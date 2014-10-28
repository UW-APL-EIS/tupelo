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

