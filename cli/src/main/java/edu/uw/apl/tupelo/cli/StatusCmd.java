package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

import edu.uw.apl.commons.devicefiles.DeviceFile;

public class StatusCmd extends Command {
	StatusCmd() {
		super( "status", "Show local device status with respect to store" );
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
		args = cl.getArgs();
		Config c = new Config();
		c.load( config );
		if( args.length == 0 ) {
			for( Config.Store cs : c.stores() ) {
				Store s = createStore( cs );
				Collection<ManagedDiskDescriptor> mdds = s.enumerate();
				for( Config.Device cd : c.devices() ) {
					boolean pushed = false;
					for( ManagedDiskDescriptor mdd : mdds ) {
						System.out.println( mdd );
					}
				}
			}
		}
		
	}
}

// eof
