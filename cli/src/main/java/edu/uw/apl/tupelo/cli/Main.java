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

/**
 * @author Stuart Maclean
 *
 * The main cmd line 'driver' for Tupelo operations.  Mimics how git
 * has a single cmd 'git', and uses subcommands.
 */

public class Main {

	static public void main( String[] args ) {
		if( args.length < 1 || args[0].equalsIgnoreCase( "-h" ) ) {
			String help = HelpCmd.buildHelp();
			System.out.println( help );
			return;
		}
		String cmd = args[0];
		Command c = Command.locate( cmd );
		if( c == null ) {
			HelpCmd.noCommand( cmd );
			return;
		}
		
		String[] subArgs = new String[args.length-1];
		System.arraycopy( args, 1, subArgs, 0, subArgs.length );
		try {
			c.invoke( subArgs );
		} catch( Exception e ) {
			System.err.println( e );
			e.printStackTrace();
		}
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
