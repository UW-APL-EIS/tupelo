package edu.uw.apl.tupelo.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

	public Config() {
		remotes = new ArrayList();
	}

	public void load( String s ) throws IOException {
		load( new StringReader( s ) );
	}

	public void load( File path ) throws IOException {
		load( new FileReader( path ) );
	}

	public void load( Reader r ) throws IOException {
		BufferedReader br = new BufferedReader( r );
		String line;
		Remote remote  = null;
		while(( line = br.readLine()) != null ) {
            Matcher m = REMOTESECTION.matcher( line );
            if( m.matches()) {
				String name = m.group( 1 ).trim();
				if( !haveRemote( name ) ) {
					remote = new Remote( name );
					remotes.add( remote );
				}
            } else if( remote != null ) {
				m = REMOTEURL.matcher( line );
				if( m.matches()) {
					String value  = m.group( 1 ).trim();
					remote.setUrl( value );
				}
            }
		}
	}

	boolean haveRemote( String name ) {
		for( Remote r : remotes ) {
			if( r.name.equals( name ) )
				return true;
		}
		return false;
	}
				   
	static public class Remote {
		Remote( String name ) {
			this.name = name;
		}
		void setUrl( String s ) {
			url = s;
		}
		
		final String name;
		String url;
	}

	private final List<Remote> remotes;
	
	static private final Pattern REMOTESECTION  =
		Pattern.compile( "[remote \"([^\"]*)\\]" );
	static private final  Pattern REMOTEURL =
		Pattern.compile( "url = (.*)" );

}

// eof
