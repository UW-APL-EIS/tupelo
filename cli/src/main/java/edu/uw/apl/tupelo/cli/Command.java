package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

/**
 * @author Stuart Maclean
 *
 */

abstract public class Command {

	protected Command( String s ) {
		name = s;
		COMMANDS.add( this );
		config = edu.uw.apl.tupelo.config.Config.DEFAULT;
	}

	protected Options commonOptions() {
		Options os = new Options();
		os.addOption( "c", true, "Config" );
		return os;
	}
	
	protected void commonParse( CommandLine cl ) {
		if( cl.hasOption( "c" ) ) {
			String s = cl.getOptionValue( "c" );
		}
	}

	abstract public void invoke( String[] args ) throws Exception;

	static Command locate( String s ) {
		for( Command c : COMMANDS ) {
			if( c.name.equals( s ) )
				return c;
		}
		return null;
	}
	
	static final List<Command> COMMANDS = new ArrayList();
	
	protected final String name;
	protected File config;
}

// eof
