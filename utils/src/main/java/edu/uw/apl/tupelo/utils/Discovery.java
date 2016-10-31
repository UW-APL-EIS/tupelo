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
package edu.uw.apl.tupelo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Discovery {

	static final Log log = LogFactory.getLog( Discovery.class );
	
	/*
	  Locate a property value given a property name.  We look in two places
	  for the properties 'file', in this order:

	  1: In a real file name $(HOME)/.tupelo

	  2: In a resource (classpath-based) named /tupelo.prp

	  The first match wins.

	  It is expected that applications can also override this discovery
	  mechanism via e.g. cmd line options.
	*/

	static public String locatePropertyValue( String prpName ) {

		String result = null;
		
		// Search 1: a fixed file = $HOME/.tupelo
		if( result == null ) {
			String userHome = System.getProperty( "user.home" );
			File dir = new File( userHome );
			File f = new File( dir, ".tupelo" );
			if( f.isFile() && f.canRead() ) {
				log.info( "Searching in file " + f + " for property " +
						  prpName );
				try {
					FileInputStream fis = new FileInputStream( f );
					Properties p = new Properties();
					p.load( fis );
					fis.close();
					result = p.getProperty( prpName );
					log.info( "Located " + result );
				} catch( IOException ioe ) {
					log.info( ioe );
				}
			}
		}

		// Search 2: a fixed resource
		if( result == null ) {
			InputStream is = Discovery.class.getResourceAsStream
				( "/tupelo.prp" );
			if( is != null ) {
				try {
					log.info( "Searching in resource for property " +
							  prpName );
					Properties p = new Properties();
					p.load( is );
					result = p.getProperty( prpName );
					log.info( "Located " + result );
					is.close();
				} catch( IOException ioe ) {
					log.info( ioe );
				}
			}
		}
		return result;
	}
}

// eof

	