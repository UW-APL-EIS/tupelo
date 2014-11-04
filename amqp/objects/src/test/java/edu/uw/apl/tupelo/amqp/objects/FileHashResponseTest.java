package edu.uw.apl.tupelo.amqp.objects;

import com.google.gson.*;

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
		gson = gb.create();
	}

	public void testNull() {
	}

	public void test1() {
		FileHashResponse r = new FileHashResponse( "md5" );
		String diskID = "seagate1234";
		Session session = Session.testSession();
		r.add( new byte[20], new ManagedDiskDescriptor( diskID, session ),
			   "/" );
		String s = gson.toJson( r );
		System.out.println( "Response: " + s );

		FileHashResponse r2 = (FileHashResponse)gson.fromJson
			( s, FileHashResponse.class );
	}

	Gson gson;
}

// eof
