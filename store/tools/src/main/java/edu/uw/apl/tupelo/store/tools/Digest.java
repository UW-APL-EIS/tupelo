package edu.uw.apl.tupelo.store.tools;

import java.io.Console;
import java.io.File;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;

import edu.uw.apl.commons.sleuthkit.image.Image;
import edu.uw.apl.commons.sleuthkit.filesys.Attribute;
import edu.uw.apl.commons.sleuthkit.filesys.Meta;
import edu.uw.apl.commons.sleuthkit.filesys.FileSystem;
import edu.uw.apl.commons.sleuthkit.filesys.DirectoryWalk;
import edu.uw.apl.commons.sleuthkit.filesys.Walk;
import edu.uw.apl.commons.sleuthkit.filesys.WalkFile;
import edu.uw.apl.commons.sleuthkit.volsys.Partition;
import edu.uw.apl.commons.sleuthkit.volsys.VolumeSystem;

/**
 * Simple Tupelo Utility: Hash some previously added ManagedDisk,
 * using tsk4j/Sleuthkit routines and a FUSE filesystem to access the
 * managed data.  Store the resultant 'Hash Info' as a Store
 * attribute.
 *
 * Note: This program makes use of fuse4j/fuse and so has an impact on
 * the filesystem as a whole.  In principle, a fuse mount point is
 * created at program start and deleted at program end.  However, if
 * user exits early (Ctrl C), we may have a lasting mount point.  To
 * delete this, do
 *
 * $ fusermount -u test-mount
 *
 * We do have a shutdown hook for the umount installed, but it appears
 * unreliable.
 *
 */

public class Digest extends Base {

	static public void main( String[] args ) {
		Digest main = new Digest();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			if( debug )
				e.printStackTrace();
			System.exit(-1);
		} finally {
			LogManager.shutdown();
		}
			  
	}

	public Digest() {
	}

	@Override
	/*
	  Am NOT calling super method, since cmd line arg processing not
	  composable.
	*/
	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "i", false, "Interactive" );

		String usage = commonUsage() + " [-i] (diskID sessionID)?";
		final String HEADER = "";
		final String FOOTER = "";
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		commonParse( os, cl, usage, HEADER, FOOTER );
		interactive = cl.hasOption( "i" );
		if( all )
			return;
		args = cl.getArgs();
		if( args.length < 2 ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
	}

	/**
	 * Ask the user, via the console, for a yes/no answer to the
	 * question of digesting the managed disk as identified in the
	 * prompt.
	 */
	private boolean proceedTest( ManagedDiskDescriptor mdd ) {
		Console c = System.console();
		if( c == null )
			return true;
		if( !interactive )
			return true;
		String line = c.readLine( "" + mdd + "? y/n " );
		if( line == null )
			return false;
		line = line.trim();
		if( line.isEmpty() )
			return false;
		return line.charAt(0) == 'y' || line.charAt(0) == 'Y';
	}

	public void start() throws Exception {
		File dir = new File( storeLocation );
		if( !dir.isDirectory() ) {
			throw new IllegalStateException
				( "Not a directory: " + storeLocation );
		}
		FilesystemStore store = new FilesystemStore( dir );
		if( debug )
			System.out.println( "Store type: " + store );

		if( all ) {
			Collection<ManagedDiskDescriptor> mdds = store.enumerate();
			List<ManagedDiskDescriptor> sorted =
				new ArrayList<ManagedDiskDescriptor>( mdds );
			Collections.sort( sorted,
							  ManagedDiskDescriptor.DEFAULTCOMPARATOR );
			for( ManagedDiskDescriptor mdd : sorted ) {
				long sz = store.size( mdd );
				boolean proceed = proceedTest( mdd );
				if( !proceed )
					continue;
				System.out.println( "Digesting: " + mdd +
									" (" + sz + " bytes)" );
				store.computeDigest( mdd );
			}
		} else {
			ManagedDiskDescriptor mdd = Utils.locateDescriptor
				( store, diskID, sessionID );
			if( mdd == null ) {
				System.err.println( "Not stored: " + diskID + "," + sessionID );
				System.exit(1);
			}
			store.computeDigest( mdd );
		}
	}

	boolean interactive;
}

// eof
