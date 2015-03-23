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

	