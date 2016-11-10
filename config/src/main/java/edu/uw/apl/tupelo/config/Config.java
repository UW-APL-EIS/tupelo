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
 * Configuration info for Tupelo, based on git's config format. A
 * configuration simply associates easy-to-use names with hard-to-use
 * names, much like git's use of 'remotes'.  We add Devices and Stores
 * in this manner.
 *
 * Used typically by the cli module's Main class.  But could be used
 * also by some new GUI front-end, hence the Config object gets its
 * own class AND package (and Maven sub-module).
 *
 * The job of the Config object is to store persistent state across
 * Tupelo sessions.  It does this by using local disk storage,
 * typically ~USER/.tupconfig.  There may be times when no such
 * storage is available.  Booting off e.g.  a live Linux/Caine CD will
 * result in no long-term storage.  While some form of RAM-fs may
 * provide persistent storage for many Tupelo sessions, once the
 * machine is powered off, any such stored configuration would be
 * lost.  Thumb-drive boots may be better and provide long term
 * storage on the tumb drive??
 */

public class Config {

	public Config() {
		stores = new ArrayList();
		devices = new ArrayList();
	}

	/**
	 * Set a backing file so the config can be persisted to some disk
	 * file.  Note: This disk storage may or may not be truly
	 * persistent across boots, depending on our current boot
	 * situation: Live CD boots likely have no persistent storage
	 * location.
	 */
	public void setBacking( File f ) {
		backing = f;
	}

	/**
	 * @return A list of all device objects in the current
	 * configuration.  Devices are added to that configuration via the
	 * addDevice method.
	 *
	 * @see addDevice
	 * @see removeDevice
	 */
	public List<Device> devices() {
		return devices;
	}

	/**
	 * @return A list of all store objects in the current
	 * configuration.  Stores are added to that configuration via the
	 * addStore method.
	 *
	 * @see addStore
	 * @see removeStore
	 */
	public List<Store> stores() {
		return stores;
	}

	/**
	 * Add a store to this configuration.  If a store with same name
	 * as to-be-added store already exists, the add is a no-op.
	 *
	 * @return true if the new store is added, false otherwise
	 */
	public boolean addStore( String name, String url ) {
		if( haveStore( name ) )
			return false;
		Store s = new Store( name );
		s.setUrl( url );
		stores.add( s );
		return true;
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

	/**
	 * Add a device to this configuration.  If a device with same name
	 * as to-be-added store already exists, the add is a no-op.
	 *
	 * @return true if the new device is added, false otherwise
	 */
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

	/**
	 * Load a configuration from its backing/persistent disk file.
	 * A backing file must have been set prior to this call.
	 *
	 * @see setBacking
	 */
	public void load() throws IOException {
		if( backing == null )
			throw new IllegalStateException( "Config.load: no backing file" );
		load( backing );
	}

	public void load( String s ) throws IOException {
		load( new StringReader( s ) );
	}

	public void load( File path ) throws IOException {
		load( new FileReader( path ) );
	}

	/**
	 * Load a configuration from any reader, that is, any object from
	 * which we can retrieve lines of text.  The Tupelo configuration
	 * stored syntax is a text style, based on git's config style
	 * (~/.gitconfig).
	 */
	public void load( Reader r ) throws IOException {
		BufferedReader br = new BufferedReader( r );
		String line;
		Store store = null;
		Device device = null;
		while(( line = br.readLine()) != null ) {
			line = line.trim();
			boolean matches = false;
			Matcher m = null;

			// Test this line is a store 'header' i.e. [store NAME]
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
			
			// Test this line is a store attribute
			if( !matches && store != null ) {
				m = STOREURL.matcher( line );
				matches = m.matches();
				if( matches ) {
					String value = m.group( 1 ).trim();
					store.setUrl( value );
					stores.add( store );
				}
			}
			
			// Test this line is a device 'header' i.e. [device NAME]
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

			// Test this line is a device path attribute
			if( !matches && device != null ) {
				m = DEVICEPATH.matcher( line );
				matches = m.matches();
				if( matches ) {
					String value = m.group( 1 ).trim();
					device.setPath( value );
					devices.add( device );
				}
			}
			
			// Test this line is a device id attribute
			if( !matches && device != null ) {
				m = DEVICEID.matcher( line );
				matches = m.matches();
				if( matches ) {
					String id = m.group( 1 ).trim();
					device.setID( id );
				}
			}

			// Test this line is a device size attribute
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
				   
	/**
	 * Store/save a configuration to its backing/persistent disk file.
	 * A backing file must have been set prior to this call.
	 *
	 * @see setBacking
	 */
	public void store() throws IOException {
		if( backing == null )
			throw new IllegalStateException( "Config.store: no backing file" );
		store( backing );
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

	/**
	 * A Config.Store class associates a friendly name, e.g. S1
	 * with a url, e.g. https://webAccessibleTupStore', much
	 * like git's remotes.
	 */
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
		public String toString() {
			// Useful for debug
			return name + " = " + url;
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
		public String toString() {
			// Useful for debug
			return name + " = " + path + " = " + id;
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

	private File backing;
	private final List<Store> stores;
	private final List<Device> devices;

	/*
	  The 'language' of the Config object when persisted to
	  backing file: a set of regular expressions.
	*/
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

	// git uses ~/.gitconfig, we follow that idea with ~/.tupconfig...
	static public File DEFAULT;
	static {
		String s = System.getProperty( "user.home" );
		File f = new File( s );
		f = new File( f, ".tupconfig" );
		DEFAULT = f;
		//		System.err.println( DEFAULT );
	}
}

// eof
