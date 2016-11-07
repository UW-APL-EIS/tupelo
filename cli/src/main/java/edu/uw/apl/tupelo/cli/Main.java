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
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

import edu.uw.apl.tupelo.config.Config;

/**
 * @author Stuart Maclean
 *
 * The main cmd line 'driver' for Tupelo operations.  Mimics how git
 * has a single cmd 'git', and uses subcommands.
 */

public class Main {

	static public void main( String[] args ) {
		if( args.length < 1 ) {
			String help = HelpCmd.buildHelp();
			System.out.println( help );
			return;
		}

		// Step 1: global options 
		CommandLineParser clp = new DefaultParser();
		CommandLine cl = null;
		boolean stopAtNonOption = true;
		try {
			cl = clp.parse( globalOptions, args, stopAtNonOption );
		} catch( ParseException neverWithStopAtNonOption ) {
		}
		if( cl.hasOption( "h" ) ) {
			String help = HelpCmd.buildHelp();
			System.out.println( help );
			return;
		}			
		File configBacking = Config.DEFAULT;
		if( cl.hasOption( "c" ) ) {
			String s = cl.getOptionValue( "c" );
			File f = new File( s );
			if( !f.canRead() ) {
				System.err.println( s + ": no such config file" );
				return;
			}
			configBacking = f;
		}
		Config config = new Config();
		config.setBacking( configBacking );
		try {
			config.load();
		} catch( IOException ioe ) {
			System.err.println( ioe );
		}
		boolean verbose = cl.hasOption( "v" );

		// Step 2: from remaining cmd line, identify command, subcommand
		args = cl.getArgs();
		if( args.length == 0 ) {
			String help = HelpCmd.buildHelp();
			System.out.println( help );
			return;
		}
		String cmdName = args[0];
		Command cmd = Command.locate( cmdName );
		if( cmd == null ) {
			HelpCmd.noCommand( cmdName );
			return;
		}
		if( cmd.hasSubCommands() ) {
			if( args.length < 2 ) {
				HelpCmd.INSTANCE.commandHelp( cmd );
				return;
			}
			String subCmdName = args[1];
			Command.Sub sub = cmd.locateSub( subCmdName );
			if( sub == null ) {
				HelpCmd.INSTANCE.commandHelp( cmd );
				return;
			}
			Options os = sub.options();
			String[] subArgs = new String[args.length-2];
			System.arraycopy( args, 2, subArgs, 0, subArgs.length );
			stopAtNonOption = false;
			try {
				cl = clp.parse( os, subArgs, stopAtNonOption );
			} catch( ParseException pe ) {
				HelpCmd.INSTANCE.commandHelp( cmd, sub );
				return;
			}
			args = cl.getArgs();
			if( args.length < sub.requiredArgs() ) {
				HelpCmd.INSTANCE.commandHelp( cmd, sub );
				return;
			}
			try {
				sub.invoke( config, verbose, cl );
			} catch( Exception e ) {
				e.printStackTrace();
			}
		} else {
			Options os = cmd.options();
			String[] subArgs = new String[args.length-1];
			System.arraycopy( args, 1, subArgs, 0, subArgs.length );
			stopAtNonOption = false;
			try {
				cl = clp.parse( os, subArgs, stopAtNonOption );
			} catch( ParseException pe ) {
				HelpCmd.INSTANCE.commandHelp( cmd );
				return;
			}
			args = cl.getArgs();
			if( args.length < cmd.requiredArgs() ) {
				HelpCmd.INSTANCE.commandHelp( cmd );
				return;
			}
			try {
				cmd.invoke( config, verbose, cl );
			} catch( Exception e ) {
				System.err.println( e );
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param cResult - a second result, this method returns TWO
	 * values (one just one if command not found).  cResult[0] is
	 * populated with the Command located in the args array.
	 *
	 * @result Index into the args array at which a valid Command
	 * name found, or -1 if none.
	 */
	static private int locateCommandIndex( String[] args,
										   Command[] cResult ) {
		for( int i = 0; i < args.length; i++ ) {
			String arg = args[i];
			for( Command c : Command.COMMANDS ) {
				if( arg.equals( c.name() ) ) {
					cResult[0] = c;
					return i;
				}
			}
		}
		return -1;
	}

	static Options globalOptions = new Options();
	static {
		globalOptions.addOption( "c", true, "Config file (~/.tupelo/config)" );
		globalOptions.addOption( "h", false, "help" );
		globalOptions.addOption( "v", false, "verbose" );
	}
	
	static {
		new HelpCmd();
		new ConfigCmd();
		new DeviceCmd();
		new StoreCmd();
		new StatusCmd();
		new InfoCmd();
		new PushCmd();
		new DigestCmd();
		new CatCmd();
		new MDFSCmd();
		new HashVSCmd();
		new HashFSCmd();
		new BodyfileCmd();
		new SearchCmd();
		new SessionCmd();
		new MBRCmd();
	}
}	


// eof
