package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.List;

/**
 * @author Stuart Maclean
 *
 * Tupelo sub-commands, invoked by Main.
 *
 * @see Main
 */

class Commands {

	static class Config extends Command {
		Config() {
			super( "config" );
		}
		@Override
		public void invoke( String[] args ) throws Exception {
			File f = edu.uw.apl.tupelo.config.Config.DEFAULT;
			edu.uw.apl.tupelo.config.Config c = new
				edu.uw.apl.tupelo.config.Config();
			c.load( f );
			c.store( System.out );
		}
	}

	static class Device extends Command {
		Device() {
			super( "device" );
		}
		@Override
		public void invoke( String[] args ) throws Exception {
			Options os = commonOptions();
			File f = edu.uw.apl.tupelo.config.Config.DEFAULT;
			edu.uw.apl.tupelo.config.Config c = new
				edu.uw.apl.tupelo.config.Config();
			c.load( f );
			String sub = "list";
			if( args.length > 0 )
				sub = args[0];
			switch( sub ) {
			case "list":
				List<edu.uw.apl.tupelo.config.Config.Device> ds
					= c.devices();
				for( edu.uw.apl.tupelo.config.Config.Device d : ds ) {
					System.out.println( d.getName() );
				}
				break;
			case "add":
				if( args.length > 2 ) {
					String name = args[1];
					String path = args[2];
					c.addDevice( name, path );
					c.store( f );
				}
				break;
			}
		}
	}
}

// eof
