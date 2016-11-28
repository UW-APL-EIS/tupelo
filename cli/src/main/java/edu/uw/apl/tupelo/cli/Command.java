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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
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

public class Command {

	/**
	 * @param synopsis - allowable command line invocations of this
	 * command, showing options and subcommands
	 */
	protected Command( String name ) {
		this.name = name;
		help = CommandHelp.help( name );
		options = new Options();
		subs = new ArrayList();
		COMMANDS.add( this );
	}
	
	String name() {
		return name;
		/*
		String s = getClass().getSimpleName();
		if( s.endsWith( "Cmd" ) ) {
			s = s.substring( 0, s.length() - 3 );
		}
		return s.toLowerCase();
		*/
	}

	public void addAlias( String s ) {
		alias = s;
	}

	protected void requiredArgs( String... requiredArgNames ) {
		requiredArgs = new ArrayList();
		for( String ran : requiredArgNames )
			requiredArgs.add( ran );
	}

	protected void optionalArg( String optionalArg ) {
		this.optionalArg = optionalArg;
	}
	
	protected void option( String opt, String description ) {
		Option o = new Option( opt, false, description );
		options.addOption( o );
	}

	protected void option( String opt, String argName, String description ) {
		Option o = new Option( opt, true, description );
		o.setArgName( argName );
		options.addOption( o );
	}
	
	String alias() {
		return alias;
	}
	
	Options options() {
		return options;
	}

	protected void addSub( String name, Lambda l, Options os,
						   String... requiredArgNames ) {
		List<String> args = new ArrayList();
		for( String ran : requiredArgNames )
			args.add( ran );
		Sub s = new Sub( name, l, os, args );
		subs.add( s );
	}

	// For Commands that don't have subcommands, override this...
	public void invoke( Config config, boolean verbose,
						CommandLine cl ) throws Exception {
	}
	
	static Command locate( String s ) {
		for( Command c : COMMANDS ) {
			if( s.equals( c.name() ) )
				return c;
			// Aliases can be null, so use as target
			if( s.equals( c.alias() ) )
				return c;
			
		}
		return null;
	}

	int requiredArgs() {
		return requiredArgs == null ? 0 : requiredArgs.size();
	}
	
	boolean hasSubCommands() {
		return subs.size() > 0;
	}
	
	Sub locateSub( String name ) {
		for( Sub s : subs )
			if( s.name.equals( name ) )
				return s;
		return null;
	}

	interface Lambda {
		public void invoke( Config c, boolean verbose,
							CommandLine cl ) throws Exception;
	}
	
	static class Sub {
		Sub( String name, Lambda l,
			 Options os, List<String> argNames ) {
			this.name = name;
			this.os = os;
			this.requiredArgs = argNames;
			this.l = l;
		}

		Options options() {
			return os;
		}
		
		int requiredArgs() {
			return requiredArgs.size();
		}
		
		void invoke( Config c, boolean verbose, CommandLine cl )
			throws Exception {
			l.invoke( c, verbose, cl );
		}
		String name;
		Options os;
		List<String> requiredArgs;
		Lambda l;
	}
	
	Store createStore( Config.Store cs ) throws IOException {
		String url = cs.getUrl();
		Store s = null;
		if( false ) {
		} else if( url.equals( "null" ) ) {
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
		} else if( path.equals( "random" ) ) {
			long readSpeed = 100 * (1L << 20);
			return new RandomDisk( cd.getSize(), readSpeed );
		} else if( path.equals( "zero" ) ) {
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

	final CommandHelp help;
	
	protected Options options;
	
	protected List<String> requiredArgs;
	protected String optionalArg;
	
	protected String alias;

	protected final String name;
	
	final List<Sub> subs;
}

// eof
