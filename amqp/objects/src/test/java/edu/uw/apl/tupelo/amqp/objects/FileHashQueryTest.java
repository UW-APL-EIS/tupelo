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