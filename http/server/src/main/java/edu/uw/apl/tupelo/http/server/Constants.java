package edu.uw.apl.tupelo.http.server;

import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonPrimitive;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class Constants {
	
	// A ManagedDiskDescriptor (diskid,session) encoded in a url path info
	static public final Pattern MDDPIREGEX = Pattern.compile
		( "(" + ManagedDiskDescriptor.DISKIDREGEX.pattern() + ")/" +
		  "(" + Session.SHORTREGEX.pattern() + ")" );

	static public final JsonSerializer<Session> SESSIONSERIALIZER =
		new JsonSerializer<Session>() {

		// for Json output of Session objects, we just use their .format method...
		@Override
		public JsonElement serialize( Session src,
									  java.lang.reflect.Type typeOfSrc,
									  JsonSerializationContext context) {
			return new JsonPrimitive( src.format() );
		}
	};

}

// eof
