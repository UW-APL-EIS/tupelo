package edu.uw.apl.tupelo.amqp.objects;

import java.lang.reflect.Type;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

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

	public void test1() {
		FileHashResponse r1 = new FileHashResponse( "md5" );
		String diskID = "seagate1234";
		Session session = Session.testSession();
		r1.add( new byte[20], new ManagedDiskDescriptor( diskID, session ),
			   "/" );

		RPCObject<FileHashResponse> rpc1 = RPCObject.asRPCObject( r1 );
		String s = gson.toJson( rpc1 );
		System.out.println( "Response: " + s );

		Type fhrType = new TypeToken<RPCObject<FileHashResponse>>(){}.getType();
		RPCObject<FileHashResponse> rpc2 = gson.fromJson( s, fhrType );
		FileHashResponse r2 = rpc2.appdata;

		// LOOK: what are we asserting ??
	}

	Gson gson;
}

// eof
