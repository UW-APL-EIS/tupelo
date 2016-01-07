package edu.uw.apl.tupelo.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stuart Maclean
 *
 * Configuration info for Tupelo, based on git's config format.
 */

public class Config {

	public Config() {
		stores = new ArrayList();
		devices = new ArrayList();
	}

	public boolean addStore( String name, String url ) {
		if( haveStore( name ) )
			return false;
		Store s = new Store( name );
		s.setUrl( url );
		stores.add( s );
		return true;
	}

	public boolean addDevice( String name, String path ) {
		if( haveDevice( name ) )
			return false;
		Device d = new Device( name );
		d.setPath( path );
		devices.add( d );
		return true;
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
		Store store = null;
		Device device = null;
		while(( line = br.readLine()) != null ) {
			line = line.trim();
			boolean matches = false;
			Matcher m = null;
			if( !matches ) {
				m = STORESECTION.matcher( line );
				matches = m.matches();
				if( matches ) {
					device = null;
					String name = m.group( 1 ).trim();
					if( !haveStore( name ) ) {
						store = new Store( name );
					}
				}
			}
			if( !matches && store != null ) {
				m = STOREURL.matcher( line );
				matches = m.matches();
				if( matches ) {
					String value = m.group( 1 ).trim();
					store.setUrl( value );
					stores.add( store );
				}
			}
			if( !matches ) {
				m = DEVICESECTION.matcher( line );
				matches = m.matches();
				if( matches ) {
					store = null;
					String name = m.group( 1 ).trim();
					if( !haveDevice( name ) ) {
						device = new Device( name );
					}
				}
			}
			if( !matches && device != null ) {
				m = DEVICEPATH.matcher( line );
				matches = m.matches();
				if( matches ) {
					String value = m.group( 1 ).trim();
					device.setPath( value );
					devices.add( device );
				}
			}
		}
	}

	boolean haveStore( String name ) {
		for( Store s : stores ) {
			if( s.name.equals( name ) )
				return true;
		}
		return false;
	}

	boolean haveDevice( String name ) {
		for( Device d : devices ) {
			if( d.name.equals( name ) )
				return true;
		}
		return false;
	}
				   
	public void store( OutputStream os ) throws IOException {
		store( new OutputStreamWriter( os ) );
	}

	public void store( File f ) throws IOException {
		store( new FileWriter( f ) );
	}
	
	public void store( Writer w ) throws IOException {
		PrintWriter pw = new PrintWriter( w );
		for( Store s : stores ) {
			pw.println( "[store \"" + s.name + "\"]" );
			pw.println( "\turl = " + s.url );
		}
		for( Device d : devices ) {
			pw.println( "[device \"" + d.name + "\"]" );
			pw.println( "\tpath = " + d.path );
		}
		pw.close();
	}
	

	static public class Store {
		Store( String name ) {
			this.name = name;
		}
		void setUrl( String s ) {
			url = s;
		}
		
		final String name;
		String url;
	}

	static public class Device {
		Device( String name ) {
			this.name = name;
		}
		void setPath( String s ) {
			path = s;
		}
		void setID( String s ) {
			id = s;
		}
		void setType( String s ) {
			type = s;
		}
		
		final String name;
		String path, id, type;
	}

	private final List<Store> stores;
	private final List<Device> devices;
	
	static private final Pattern STORESECTION  =
		Pattern.compile( "\\[store \"([^\"]+)\"\\]" );
	static private final Pattern STOREURL =
		Pattern.compile( "url = (.+)" );

	static private final Pattern DEVICESECTION  =
		Pattern.compile( "\\[device \"([^\"]+)\"\\]" );
	static private final  Pattern DEVICEPATH =
		Pattern.compile( "path = (.+)" );

}

// eof
