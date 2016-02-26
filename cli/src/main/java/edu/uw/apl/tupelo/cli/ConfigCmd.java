package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;

import edu.uw.apl.tupelo.config.Config;

public class ConfigCmd extends Command {
	ConfigCmd() {
		super( "config", "Print configuration" );
	}

	@Override
	public void invoke( String[] args ) throws Exception {
		File f = Config.DEFAULT;
		Config c = new Config();
		try {
			c.load( f );
		} catch( IOException ioe ) {
		}
		c.store( System.out );
	}
}

// eof

