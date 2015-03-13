package edu.uw.apl.tupelo.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adhering to semantic versioning: http://semver.org
 *
 * TODO: implement the 'compatible logic' ie we are running code
 * tagged with some version X.  We find an artifact on disk, i.e. a
 * ManagedDisk, with included version Y.  Can we reliably
 * read/understand that file?
 */
public class Version {

	static public int MAJOR = 0;
	static public int MINOR = 0;
	static public int PATCH = 0;

	static public int VERSION;
	
	static private final Pattern REGEX =
		Pattern.compile( "(\\d+)\\.(\\d+)\\.(\\d+)" );

	/*
	  Attempt to extract version info from the jar, itself populated
	  by Maven with version info from the pom.
	*/
	static {
		try {
			Package p = Version.class.getPackage();
			String s = p.getImplementationVersion();
			if( s == null )
				s = p.getSpecificationVersion();
			if( s != null ) {
				Matcher m = REGEX.matcher( s );
				if( m.matches() ) {
					MAJOR = Integer.parseInt( m.group(1) );
					MINOR = Integer.parseInt( m.group(2) );
					PATCH = Integer.parseInt( m.group(3) );
				}
			}
		} catch( Exception e ) {
			throw new ExceptionInInitializerError( e );
		}
		VERSION = ((MAJOR & 0xff) << 16) |
			((MINOR & 0xff) << 8) |
			(PATCH & 0xff);

		// Do we really want to report this ??
		if( false )
			System.out.println( "Version: " +
								MAJOR + "." + MINOR + "." + PATCH );
		
	}
	
}

// eof
