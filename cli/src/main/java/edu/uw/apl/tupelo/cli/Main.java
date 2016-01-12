package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
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
		if( args.length < 1 ) {
			System.out.println( USAGE );
			System.exit(1);
		}
		String cmd = args[0];
		Command c = Command.locate( cmd );
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
		new Commands.Config();
		new Commands.Device();
	}
	
	static private String USAGE = "help TODO";
}

// eof
