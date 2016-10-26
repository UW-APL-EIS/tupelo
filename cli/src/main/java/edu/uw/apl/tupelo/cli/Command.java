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
package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;

import edu.uw.apl.tupelo.config.Config;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;
import edu.uw.apl.tupelo.store.null_.NullStore;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.model.PhysicalDisk;
import edu.uw.apl.tupelo.model.VirtualDisk;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.RandomDisk;
import edu.uw.apl.tupelo.model.ZeroDisk;

/**
 * @author Stuart Maclean
 *
 */

abstract public class Command {

	/**
	 * @param synopsis - allowable command line invocations of this
	 * command, showing options and subcommands
	 */
	protected Command( String summary, String synopsis, String description ) {
		this.summary = summary;
		this.synopsis = synopsis;
		this.description = description;
		config = Config.DEFAULT;
		subs = new ArrayList();
		COMMANDS.add( this );
	}
	
	protected Command( String summary, String synopsis ) {
		this( summary, synopsis, "DESCRIPTION" );
	}

	protected Command( String summary ) {
		this( summary, "SYNOPSIS", "DESCRIPTION" );
	}

	
	String name() {
		String s = getClass().getSimpleName();
		if( s.endsWith( "Cmd" ) ) {
			s = s.substring( 0, s.length() - 3 );
		}
		return s.toLowerCase();
	}

	public void addAlias( String s ) {
		alias = s;
	}

	String alias() {
		return alias;
	}
	
	String summary() {
		return summary;
	}

	String synopsis() {
		return synopsis;
	}

	String description() {
		return description;
	}
	
	public void addSub( String name, Lambda l ) {
		Sub s = new Sub( name, l );
		subs.add( s );
		if( subs.size() == 1 )
			subDefault = s;
	}
	
	protected Options commonOptions() {
		Options os = new Options();
		os.addOption( "c", true, "Config" );
		return os;
	}
	
	protected void commonParse( CommandLine cl ) {
		if( cl.hasOption( "c" ) ) {
			String s = cl.getOptionValue( "c" );
			config = new File( s );
		}
	}

	public void invoke( String[] args ) throws Exception {
		Options os = commonOptions();
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
			commonParse( cl );
		} catch( ParseException pe ) {
			//	printUsage( os, usage, HEADER, FOOTER );
			//System.exit(1);
		}
		args = cl.getArgs();
		if( args.length == 0 ) {
			Sub sub = subDefault;
			if( sub == null ) {
				HelpCmd.INSTANCE.commandHelp( this );
				return;
			}
			Config c = new Config();
			c.load( config );
			sub.invoke( null, args, c );
			return;
		}
		String subName = args[0];

		Sub s = locateSub( subName );
		if( s == null ) {
			HelpCmd.INSTANCE.commandHelp( this );
			return;
		}
		Config c = new Config();
		c.load( config );
		String[] subArgs = new String[args.length-1];
		if( args.length > 1 )
			System.arraycopy( args, 1, subArgs, 0, subArgs.length );
		s.invoke( null, subArgs, c ); 
	}

	static Command locate( String s ) {
		for( Command c : COMMANDS ) {
			if( s.equals( c.name() ) )
				return c;
			// Aliases can be null, so use s as target
			if( s.equals( c.alias() ) )
				return c;
			
		}
		return null;
	}

	private Sub locateSub( String name ) {
		for( Sub s : subs )
			if( s.name.equals( name ) )
				return s;
		return null;
	}
	
	interface Lambda {
		public void invoke( CommandLine cl, String[] args,
							Config c ) throws Exception;
	}
	
	static class Sub {
		Sub( String name, Lambda l ) {
			this.name = name;
			this.l = l;
		}
		void invoke( CommandLine cl, String[] args, Config c )
			throws Exception {
			l.invoke( cl, args, c );
		}
		String name;
		Lambda l;
	}
	
	Store createStore( Config.Store cs ) {
		String url = cs.getUrl();
		Store s = null;
		if( false ) {
		} else if( url.equals( "/dev/null" ) ) {
			s = new NullStore();
		} else if( url.startsWith( "http" ) ) {
			s = new HttpStoreProxy( url );
		} else {
			File dir = new File( url );
			if( !dir.isDirectory() ) {
				throw new IllegalStateException
					( "Not a directory: " + url );
			}
			s = new FilesystemStore( dir );
		}
		return s;
	}

	UnmanagedDisk createUnmanagedDisk( Config.Device cd ) throws IOException {
		String path = cd.getPath();
		File f = new File( path );
		if( false ) {
		} else if( path.equals( "/dev/random" ) ) {
			long readSpeed = 100 * (1L << 20);
			return new RandomDisk( cd.getSize(), readSpeed );
		} else if( path.equals( "/dev/zero" ) ) {
			long readSpeed = 100 * (1L << 20);
			return new ZeroDisk( cd.getSize(), readSpeed );
		} else if( path.startsWith( "/dev/" ) ) {
			return new PhysicalDisk( f );
		} else if( VirtualDisk.likelyVirtualDisk( f ) ) {
			return new VirtualDisk( f );
		} else {
			// LOOK: give better ID, via some cmd line option -n ??
			return new DiskImage( f );
		}
		return null;
	}

	static final List<Command> COMMANDS = new ArrayList();
	
	protected File config;
	protected String alias;
	
	protected final String summary, synopsis, description;
	protected final List<Sub> subs;
	protected Sub subDefault;
}

// eof
