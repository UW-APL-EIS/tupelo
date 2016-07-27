package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.config.Config;
	
public class StoreCmd extends Command {
	StoreCmd() {
		super( "store", "List, create or delete stores" );
		addSub( "list", "", 0, new Lambda() {
				public void invoke( CommandLine cl ) throws Exception {
					Config c = new Config();
					c.load( StoreCmd.this.config );
					List<Config.Store> ss = c.stores();
					for( Config.Store s : ss ) {
						System.out.println( s.getName() );
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
			for( Config.Store s : c.stores() ) {
				System.out.println( s.getName() );
				System.out.println( " path = " + s.getUrl() );
			}
			break;
		case "add":
			if( args.length > 2 ) {
				String name = args[1];
				String url = args[2];
				c.addStore( name, url );
				c.store( config );
			}
			break;
		case "remove":
			if( args.length > 1 ) {
				String name = args[1];
				c.removeStore( name );
				c.store( config );
			}
			break;
		}
	}
}

// eof
