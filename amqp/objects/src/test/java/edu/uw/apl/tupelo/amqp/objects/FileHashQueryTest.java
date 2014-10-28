package edu.uw.apl.tupelo.amqp.objects;


import com.google.gson.*;

public class FileHashQueryTest extends junit.framework.TestCase {

	protected void setUp() {
		GsonBuilder gb = new GsonBuilder();
		gb.disableHtmlEscaping();
		gb.registerTypeAdapter(byte[].class,
							   new JSONSerializers.MessageDigestSerializer() );
		gson = gb.create();
	}

	public void testNull() {
	}

	public void test1() {
		FileHashQuery q = new FileHashQuery( "sha1" );
		q.add( new byte[20] );
		q.add( new byte[20] );
		String s = gson.toJson( q );
		System.out.println( "Request: " + s );

		FileHashQuery q2 = (FileHashQuery)gson.fromJson
			( s, FileHashQuery.class );
		report( q2 );
	}

	private void report( FileHashQuery q ) {
		System.out.println( q.algorithm );
		System.out.println( q.hashes.size() );
	}
	
	Gson gson;
}

// eof