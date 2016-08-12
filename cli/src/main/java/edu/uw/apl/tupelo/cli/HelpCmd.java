package edu.uw.apl.tupelo.cli;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.*;

public class HelpCmd extends Command {

	HelpCmd() {
		super( "Explain available commands", "command?",
			   "Provide help on how to use each Tupelo command" );
		INSTANCE = this;
	}
	
	@Override
	public void invoke( String[] args ) throws Exception {
		if( args.length == 0 ) {
			String help = buildHelp();
			System.out.println( help );
			return;
		}
		String cmd = args[0];
		Command c = Command.locate( cmd );
		if( c == null ) {
			noCommand( cmd );
			return;
		}
		commandHelp( c );
	}

	static void noCommand( String cmd ) {
		System.out.println( "'" + cmd + "' is not a Tupelo command. " +
							"See 'help' command." );
	}
	
	void commandHelp( Command c ) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		pw.println();
		pw.println( "NAME" );
		pw.println( "  " + COMMANDNAME + " " + c.name() + " - " +
					c.summary() );
		pw.println();
		pw.println( "SYNOPSIS" );
		pw.println( "  " + COMMANDNAME + " " + c.name() + " " +
					c.synopsis() );
		pw.println();
		pw.println( "DESCRIPTION" );
		pw.println( "  " + c.description() );
		String s = sw.toString();
		System.out.println( s );
	}
	
	static String buildHelp() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		pw.println( "Usage: " + COMMANDNAME + " <command> [<args>]" );
		pw.println();
		pw.println( "Commands" );
		for( Command c : Command.COMMANDS ) 
			pw.printf( "  %-15s %s\n", c.name(), c.summary() );
		return sw.toString();
	}

	static HelpCmd INSTANCE;
	
	static final String COMMANDNAME = "tup";
}

// eof
