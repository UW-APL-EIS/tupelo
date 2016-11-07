/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.WordUtils;

import edu.uw.apl.tupelo.config.Config;

public class HelpCmd extends Command {

	HelpCmd() {
		super( "help" );
		/*
		super( "Explain available commands", "command?",
			   "Provide help on how to use each Tupelo command" );
		*/
		INSTANCE = this;
	}

	@Override
	public void invoke( Config config, boolean verbose,
						CommandLine cl ) throws Exception {
		String[] args = cl.getArgs();

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
		System.err.println( "'" + cmd + "' is not a Tupelo command. " +
							"See 'help' command." );
	}
	
	void commandHelp( Command c, Command.Sub sub ) {
		CommandHelp h = c.help;

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		pw.println();
		pw.println( "NAME " + c.name() + " " + sub.name );
		String s = sw.toString();
		System.out.println( s );
	}
	
	void commandHelp( Command c ) {
		CommandHelp h = c.help;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		pw.println();
		pw.println( "NAME" );
		pw.println( "  " + COMMANDNAME + " " + c.name() + " - " +
					h.summary() );
		pw.println();
		pw.println( "SYNOPSIS" );
		if( c.hasSubCommands() ) {
			for( Command.Sub s : c.subs ) {
				pw.print( "  " + COMMANDNAME + " " + c.name() + " " +
							s.name );
				Options os = s.options();
				Collection<Option> ops = os.getOptions();
				for( Option o : ops ) {
					pw.print( " [-" + o.getOpt() );
					if( o.hasArg() )
						pw.print( " " + o.getArgName() );
					pw.print( "]" );
				}
				List<String> requiredArgs = s.requiredArgs;
				for( String ra : requiredArgs ) {
					pw.print( " <" + ra + ">" );
				}
				pw.println();
			}
		} else {
			pw.print( "  " + COMMANDNAME + " " + c.name() );
			Options os = c.options();
			Collection<Option> ops = os.getOptions();
			for( Option o : ops ) {
				pw.print( " [-" + o.getOpt() );
				if( o.hasArg() )
					pw.print( " " + o.getArgName() );
				pw.print( "]" );
			}
			List<String> requiredArgs = c.requiredArgs;
			for( String ra : requiredArgs ) {
				pw.print( " <" + ra + ">" );
			}
			if( c.optionalArg != null )
				pw.print( " <" + c.optionalArg + "?>" );
				
			pw.println();
		}
		pw.println();
		pw.println( "DESCRIPTION" );
		String descFmt = format( h.description(), 2 );
		pw.println( descFmt );
		//		pw.println();
		pw.println( "OPTIONS" );
		pw.println( "  " + "TODO" );
		List<String> examples = h.examples();
		if( !examples.isEmpty() ) {
			pw.println();
			pw.println( "EXAMPLES" );
			for( String s : examples ) {
				// Suppress COMMANDNAME if want to print a shell command
				if( s.startsWith( "!" ) )
					pw.println( "  $ " + s.substring(1) );
				else
					pw.println( "  $ " + COMMANDNAME + " " + s );
				//				pw.println();
			}
		}
		String s = sw.toString();
		System.out.println( s );
	}

	
	static String format( String s, int indent ) {
		String wrapped = WordUtils.wrap( s, 70 );
		String[] lines = wrapped.split( LS );
		StringBuilder sb = new StringBuilder();
		for( int i = 0; i < lines.length; i++ )
			sb.append( INDENTS[indent] + lines[i] + LS );
		return sb.toString();
	}
	
	static String buildHelp() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		pw.println( "Usage: " + COMMANDNAME + " " + GLOBALARGS +
				    " <command> [<args>]" );
		pw.println();
		pw.println( "Commands" );
		for( Command c : Command.COMMANDS ) 
			pw.printf( "  %-15s %s\n", c.name(), c.help.summary() );
		return sw.toString();
	}

	/*
	  So that other commands may use the Help command, give it a name
	*/
	static HelpCmd INSTANCE;
	
	static final String COMMANDNAME = "tup";

	static final String GLOBALARGS = "[-c configFile] [-v]" ;

	static final private String LS = System.getProperty( "line.separator" );

	static String[] INDENTS = { "", " ", "  ", "   " };
}

// eof
