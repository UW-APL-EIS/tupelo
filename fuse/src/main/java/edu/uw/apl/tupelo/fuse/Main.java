package edu.uw.apl.tupelo.fuse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.cli.*;

import fuse.FuseMount;

import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

/**
 * A command-line entry point to creating a ManagedDiskFileSystem.
 * Takes a Tupelo store by local file name.  Various command line
 * options enable dryrun, verbose etc. To run:
 *
 * <pre>
 * java edu.uw.apl.tupelo.fuse.Main -s /path/to/tupeloStore mountPoint
 * </pre>
 *
 * where the mount point directory must exist a priori.
 * 
 */

 public class Main {


	public static void main(String[] args) throws Exception {
		Options os = new Options();
		os.addOption( "n", false,
					  "dryrun, show the filesystem but skip the mount" );
		os.addOption( "s", true,
					  "Store directory. Defaults to " + STORELOCATIONDEFAULT );
		os.addOption( "v", false, "verbose" );
		final String USAGE = Main.class.getName() + 
			" [-n] [-s storeLocation] [-v] mountPoint";
		final String HEADER = "";
		final String FOOTER = "";
		
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( Exception e ) {
			System.err.println( e );
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		String storeLocation = STORELOCATIONDEFAULT;
		boolean dryrun = cl.hasOption( "n" );
		boolean verbose = cl.hasOption( "v" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}

		args = cl.getArgs();
		if( args.length < 1 ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		
		File dir = new File( storeLocation );
		if( !dir.isDirectory() ) {
			throw new IllegalStateException
				( "Not a directory: " + storeLocation );
		}
		FilesystemStore store = new FilesystemStore( dir );
		
		File mount = new File( args[0] );
		if( !mount.isDirectory() ) {
			System.err.println( "Mount point " + mount +
								" not a directory" );
			System.exit(-1);
		}

		ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		boolean ownThread = false;
		mdfs.mount( mount, ownThread );
	}

	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	 static final String STORELOCATIONDEFAULT = "./test-store";
}

// eof
