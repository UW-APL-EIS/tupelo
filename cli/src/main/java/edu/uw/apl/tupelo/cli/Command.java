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
			if( c.name().equals( s ) )
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

	protected final String summary, synopsis, description;
	protected final List<Sub> subs;
	protected Sub subDefault;
}

// eof
