package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.config.Config;
	
public class StoreCmd extends Command {

	StoreCmd() {
		super( "List, create or delete stores" );
		addSub( "list", new Lambda() {
				public void invoke( CommandLine cl, String[] args, Config c )
					throws Exception {
					list( c );
				}
			} );
	}

	private void list( Config c ) {
		for( Config.Store s : c.stores() ) {
			System.out.println( s.getName() );
			System.out.println( " path = " + s.getUrl() );
		}
	}

	private void add( CommandLine cl, String[] args, Config c )
		throws Exception {
		if( args.length >= 2 ) {
			String name = args[0];
			String url = args[1];
			c.addStore( name, url );
			c.store( config );
		}
	}
	
	private void remove( CommandLine cl, String[] args, Config c )
		throws Exception {
		if( args.length >= 1 ) {
			String name = args[0];
			c.removeStore( name );
			c.store( config );
		}
	}

}

// eof
