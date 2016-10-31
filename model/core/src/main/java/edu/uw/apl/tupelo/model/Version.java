/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
