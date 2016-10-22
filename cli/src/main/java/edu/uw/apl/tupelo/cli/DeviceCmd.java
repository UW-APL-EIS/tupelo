package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.RandomDisk;
import edu.uw.apl.tupelo.model.ZeroDisk;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.PhysicalDisk;
import edu.uw.apl.tupelo.model.VirtualDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.config.Config;


import edu.uw.apl.commons.devicefiles.DeviceFile;

public class DeviceCmd extends Command {
	DeviceCmd() {
		super( "List, create or delete devices", "list | add | remove" );
		addSub( "list", new Lambda() {
				public void invoke( CommandLine cl, String[] args,
									Config c ) throws Exception {
					list( c );
				}
			} );
		addSub( "add", new Lambda() {
				public void invoke( CommandLine cl, String[] args,
									Config c ) throws Exception {
					add( cl, args, c );
				}
			} );
		addSub( "remove", new Lambda() {
				public void invoke( CommandLine cl, String[] args,
									Config c ) throws Exception {
					remove( cl, args, c );
				}
			} );
	}

	private void list( Config c ) {
		List<Config.Device> ds = c.devices();
		for( Config.Device d : ds ) {
			System.out.println( d.getName() );
			System.out.println( " path = " + d.getPath() );
			System.out.println( " id   = " + d.getID() );
			System.out.println( " size = " + d.getSize() );
		}
	}

	private void add( CommandLine cl, String[] args, Config c )
		throws Exception {

		if( args.length < 2 ) {
			HelpCmd.INSTANCE.commandHelp( this );
			return;
		}
		
		String name = args[0];
		String path = args[1];
		UnmanagedDisk ud = null;
		if( false ) {
		} else if( path.equals( "/dev/random" ) ) {
			long log2size = 30L;
			if( args.length > 2 ) {
				try {
					log2size = Long.parseLong( args[2] );
				} catch( NumberFormatException nfe ) {
				}
			} else {
				System.out.println( "Using log2size: " + log2size );
			}
			long readSpeed = 100 * (1L << 20);
			long size = 1 << log2size;
			RandomDisk rd = new RandomDisk( size, readSpeed );
			ud = rd;
		} else if( path.equals( "/dev/zero" ) ) {
			long log2size = 30L;
			if( args.length > 2 ) {
				try {
					log2size = Long.parseLong( args[2] );
				} catch( NumberFormatException nfe ) {
				}
			} else {
				System.out.println( "Using log2size: " + log2size );
			}
			long readSpeed = 100 * (1L << 20);
			long size = 1 << log2size;
			ZeroDisk zd = new ZeroDisk( size, readSpeed );
			ud = zd;
		} else if( path.startsWith( "/dev/" ) ) {
			File f = new File( path );
			PhysicalDisk pd = new PhysicalDisk( f );
			ud = pd;
		} else if( path.endsWith( ".dd" ) ) {
			File f = new File( path );
			DiskImage di = new DiskImage( f );
			ud = di;
		} else if( VirtualDisk.likelyVirtualDisk( new File(path) ) ) {
			File f = new File( path );
			VirtualDisk vd = new VirtualDisk( f );
			ud = vd;
		}
		Config.Device d = c.addDevice( name, path );
		if( ud != null ) {
			d.setID( ud.getID() );
			d.setSize( ud.size() );
			c.store( config );
		} else {
			System.err.println( path + ": cannot process" );
		}
	}

	private void remove( CommandLine cl, String[] args, Config c )
		throws Exception {

		if( args.length < 1 ) {
			HelpCmd.INSTANCE.commandHelp( this );
			return;
		}
		String name = args[0];
		c.removeDevice( name );
		c.store( config );
	}
}

// eof
