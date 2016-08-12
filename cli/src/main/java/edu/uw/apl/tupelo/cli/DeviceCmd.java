package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.RandomDisk;
import edu.uw.apl.tupelo.model.ZeroDisk;
import edu.uw.apl.tupelo.config.Config;

import edu.uw.apl.commons.devicefiles.DeviceFile;

public class DeviceCmd extends Command {
	DeviceCmd() {
		super( "List, create or delete devices", "list | add | remove" );
		addSub( "list", "", 0, new Lambda() {
				public void invoke( CommandLine cl ) throws Exception {
					Config c = new Config();
					c.load( DeviceCmd.this.config );
					List<Config.Device> ds = c.devices();
					for( Config.Device d : ds ) {
						System.out.println( d.getName() );
					}
				}
			} );
	}
	
	@Override
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
		Config c = new Config();
		c.load( config );
		String sub = "list";
		if( args.length > 0 )
			sub = args[0];
		switch( sub ) {
		case "list":
			for( Config.Device d : c.devices() ) {
				System.out.println( d.getName() );
				System.out.println( " path = " + d.getPath() );
				System.out.println( " id   = " + d.getID() );
				System.out.println( " size = " + d.getSize() );
			}
				break;
		case "add":
			if( args.length > 2 ) {
				String name = args[1];
				String path = args[2];
				File f = new File( path );
				if( !f.exists() ) {
					System.err.println( "Not a file: " + f );
					return;
				}
				String id = null;
				long size = 0;
				if( false ) {
				} else if( path.equals( "/dev/random" ) ) {
					long log2size = 30L;
					if( args.length > 3 ) {
						try {
							log2size = Long.parseLong( args[3] );
						} catch( NumberFormatException nfe ) {
						}
					} else {
						System.out.println( "Using log2size: " + log2size );
					}
					long readSpeed = 100 * (1L << 20);
					size = 1 << log2size;
					RandomDisk rd = new RandomDisk( size, readSpeed );
					id = rd.getID();
				} else if( path.equals( "/dev/zero" ) ) {
					long log2size = 30L;
					if( args.length > 3 ) {
						try {
							log2size = Long.parseLong( args[3] );
						} catch( NumberFormatException nfe ) {
						}
					} else {
						System.out.println( "Using log2size: " + log2size );
					}
					long readSpeed = 100 * (1L << 20);
					size = 1 << log2size;
					ZeroDisk zd = new ZeroDisk( size, readSpeed );
					id = zd.getID();
				} else if( path.startsWith( "/dev/" ) ) {
					try {
						DeviceFile df = new DeviceFile( f );
						id = df.getID();
						System.out.println( id);
						size = df.size();
					} catch( Throwable t ) {
						t.printStackTrace();
						id = path;
					}
				}
				Config.Device d = c.addDevice( name, path );
				if( d != null ) {
					d.setID( id );
					d.setSize( size );
				}
				c.store( config );
			}
			break;
		case "remove":
			if( args.length > 1 ) {
				String name = args[1];
				c.removeDevice( name );
				c.store( config );
			}
			break;
		}
	}
}

// eof
