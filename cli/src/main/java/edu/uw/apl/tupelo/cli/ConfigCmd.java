package edu.uw.apl.tupelo.cli;

import java.io.File;

import edu.uw.apl.tupelo.config.Config;

public class ConfigCmd extends Command {
	ConfigCmd() {
		super( "config", "Print configuration" );
	}

	@Override
	public void invoke( String[] args ) throws Exception {
		File f = Config.DEFAULT;
		Config c = new Config();
		c.load( f );
		c.store( System.out );
	}
}

// eof

