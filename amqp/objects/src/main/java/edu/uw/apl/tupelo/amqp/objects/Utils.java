package edu.uw.apl.tupelo.amqp.objects;

import com.google.gson.*;

import edu.uw.apl.tupelo.model.Session;

/**
 * Centralize the creation of GsonBuilder, Gson objects used
 * throughout the Tupelo AMQP codebase.  
 */

public class Utils {
	
	static public Gson createGson( boolean withPrettyPrinting ) {
		GsonBuilder gb = new GsonBuilder();
		if( withPrettyPrinting )
			gb.setPrettyPrinting();
		gb.serializeNulls();
		gb.disableHtmlEscaping();
		// Special json encoding of Session objects
		gb.registerTypeAdapter(Session.class,
							   new JSONSerializers.SessionSerializer() );
		// Special json encoding of hashes (byte arrays) into hex strings
		gb.registerTypeAdapter(byte[].class,
							   new JSONSerializers.MessageDigestSerializer() );
		return gb.create();
	}
}

// eof

