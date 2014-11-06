package edu.uw.apl.tupelo.amqp.objects;

import java.lang.reflect.Type;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class RPCObjectTest extends junit.framework.TestCase {

	protected void setUp() {
		GsonBuilder gb = new GsonBuilder();
		gb.serializeNulls();
		gb.disableHtmlEscaping();
		gson = gb.create();
	}

	public void testNull() {
	}

	public void test1() {
		long l1 = 10;
		RPCObject<Long> rpc1 = RPCObject.asRPCObject( l1 );
		String s = gson.toJson( rpc1 );
		System.out.println( "Dummy: " + s );

		Type t = new TypeToken<RPCObject<Long>>(){}.getType();
		RPCObject<Long> rpc2 = gson.fromJson( s, t );
		long l2 = rpc2.appdata;
		assertEquals( l1, l2 );
	}
	
	Gson gson;
}

// eof