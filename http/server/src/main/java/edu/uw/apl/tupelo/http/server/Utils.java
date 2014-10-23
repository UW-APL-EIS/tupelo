package edu.uw.apl.tupelo.http.server;

import javax.servlet.http.HttpServletRequest;

public class Utils {

	static boolean acceptsJavaObjects( HttpServletRequest req ) {
		String h = req.getHeader( "Accept" );
		if( h == null )
			return false;
		return h.indexOf( "application/x-java-serialized-object" ) > -1;
	}
						 
	static boolean acceptsJson( HttpServletRequest req ) {
		String h = req.getHeader( "Accept" );
		if( h == null )
			return false;
		return h.indexOf( "application/json" ) > -1;
	}
}

// eof
