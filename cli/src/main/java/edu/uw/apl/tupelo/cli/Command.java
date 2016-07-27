package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

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

	protected Command( String name, String summary ) {
		this.name = name;
		this.summary = summary;
		config = Config.DEFAULT;
		subs = new ArrayList();
		COMMANDS.add( this );
	}

	String name() {
		return name;
	}

	String summary() {
		return summary;
	}
	
	public void addSub( String name, String usage, int requiredArgs,
						Lambda l ) {
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
	
	abstract public void invoke( String[] args ) throws Exception;

	static Command locate( String s ) {
		for( Command c : COMMANDS ) {
			if( c.name.equals( s ) )
				return c;
		}
		return null;
	}

	interface Lambda {
		public void invoke( CommandLine cl ) throws Exception;
	}
	
	static class Sub {
		Sub( String name, String usage, int requiredArgs,
			 Lambda l ) {
		}
	}
	
	static final List<Command> COMMANDS = new ArrayList();
	
	protected final String name, summary;
	protected final List<Sub> subs;
	protected File config;
}

// eof
