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

	public List<Device> devices() {
		return devices;
	}

	public List<Store> stores() {
		return stores;
	}
	
	public Store removeStore( String name ) {
		for( int i = 0; i < stores.size(); i++ ) {
			Store s = stores.get(i);
			if( s.name.equals( name ) ) {
				return stores.remove(i);
			}
		}
		return null;
	}

	public boolean addStore( String name, String url ) {
		if( haveStore( name ) )
			return false;
		Store s = new Store( name );
		s.setUrl( url );
		stores.add( s );
		return true;
	}

	public Device addDevice( String name, String path ) {
		if( haveDevice( name ) )
			return null;
		Device d = new Device( name );
		d.setPath( path );
		devices.add( d );
		return d;
	}
	
	public Device removeDevice( String name ) {
		for( int i = 0; i < devices.size(); i++ ) {
			Device d = devices.get(i);
			if( d.name.equals( name ) ) {
				return devices.remove(i);
			}
		}
		return null;
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
			if( !matches && device != null ) {
				m = DEVICEID.matcher( line );
				matches = m.matches();
				if( matches ) {
					String id = m.group( 1 ).trim();
					device.setID( id );
				}
			}
			if( !matches && device != null ) {
				m = DEVICESIZE.matcher( line );
				matches = m.matches();
				if( matches ) {
					String szS = m.group( 1 ).trim();
					long sz = Long.parseLong( szS );
					device.setSize( sz );
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
			pw.println( "\tid = " + d.id );
			pw.println( "\tsize = " + d.size );
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

		public String getName() {
			return name;
		}
		public String getUrl() {
			return url;
		}
		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals( Object o ) {
			if( o == this )
				return true;
			if( !( o instanceof Store ) )
				return false;
			Store that = (Store)o;
			return this.name.equals( that.name );
		}
		
		final String name;
		String url;
	}

	static public class Device {
		Device( String name ) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setPath( String s ) {
			path = s;
		}
		public String getPath() {
			return path;
		}
		public void setID( String s ) {
			id = s;
		}
		public String getID() {
			return id;
		}
		public void setSize( long l ) {
			size = l;
		}
		public long getSize() {
			return size;
		}
		public void setType( String s ) {
			type = s;
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals( Object o ) {
			if( o == this )
				return true;
			if( !( o instanceof Device ) )
				return false;
			Device that = (Device)o;
			return this.name.equals( that.name );
		}
		
		final String name;
		String path, id, type;
		long size;
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
	static private final  Pattern DEVICEID =
		Pattern.compile( "id = (.+)" );
	static private final  Pattern DEVICESIZE =
		Pattern.compile( "size = (\\d+)" );

	static public File DEFAULT;
	static {
		String s = System.getProperty( "user.home" );
		File f = new File( s );
		f = new File( f, ".tupelo" );
		f.mkdirs();
		f = new File( f, "config" );
		DEFAULT = f;
		//		System.err.println( DEFAULT );
	}
}

// eof
