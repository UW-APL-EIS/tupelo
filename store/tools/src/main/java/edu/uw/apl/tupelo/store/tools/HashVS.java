package edu.uw.apl.tupelo.store.tools;

import java.io.File;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
 * Simple Tupelo Utility: Walk some previously added ManagedDisk,
 * using tsk4j/Sleuthkit routines and a FUSE filesystem to access the
 * managed data.  The 'walk' visits every UNALLOCATED partition in the
 * VolumeSystem and produces a sha1 hash of its content.  We visit
 * only unallocated parts of the disk since we need much finer grained
 * inspection of filesystems.  Different results for a hash at time
 * T1, T2 over a partition containing a filesystem would not tell you
 * WHICH file(s) changed, so see e.g. {@link HashFS} for that.
 *
 * If the -a option is supplied, we store the result (a multi-line
 * formatted string) back to the Tupelo store as an attribute of the
 * managed disk.
 *
 * Note: This program makes use of fuse4j/fuse and so has an impact on
 * the host filesystem as a whole.  In principle, a fuse mount point
 * is created at program start and deleted at program end.  However,
 * if user exits early (Ctrl C), we may have a lasting mount point.
 * To delete this, do
 *
 * $ fusermount -u test-mount
 *
 * We do have a shutdown hook for the umount installed, but it appears
 * unreliable.
 *
 */

public class HashVS extends Base {

	static public void main( String[] args ) {
		HashVS main = new HashVS();
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

	public HashVS() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "a", false,
					  "Store hash result as managedDisk attribute" );
		os.addOption( "v", false, "Verbose" );

		String usage = commonUsage() + " [-v] diskID sessionID";
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

		addAttribute = cl.hasOption( "a" );
		verbose = cl.hasOption( "v" );
		args = cl.getArgs();
		if( args.length < 2 ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
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

		Collection<ManagedDiskDescriptor> stored = store.enumerate();
		System.out.println( "Stored: " + stored );

		ManagedDiskDescriptor mdd = locateDescriptor( store, diskID, sessionID );
		if( mdd == null ) {
			System.err.println( "Not stored: " + diskID + "," + sessionID );
			System.exit(1);
		}
			
		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		
		final File mountPoint = new File( "test-mount" );
		mountPoint.mkdirs();
		mountPoint.deleteOnExit();
		if( debug )
			System.out.println( "Mounting '" + mountPoint + "'" );
		mdfs.mount( mountPoint, true );
		Runtime.getRuntime().addShutdownHook( new Thread() {
				public void run() {
					if( debug )
						System.out.println( "Unmounting '" + mountPoint + "'" );
					try {
						mdfs.umount();
					} catch( Exception e ) {
						System.err.println( e );
					}
				}
			} );
		
		// LOOK: wait for the fuse mount to finish.  Grr hate arbitrary sleeps!
		Thread.sleep( 1000 * 2 );

		File f = mdfs.pathTo( mdd );
		System.out.println( "Located Managed Data: " + f );
		Image i = new Image( f );
		try {
			VolumeSystem vs = new VolumeSystem( i );
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter( sw );
				List<Partition> ps = vs.getPartitions();
				for( Partition p : ps ) {
					log.info( "" + p.start() + " to " + p.length() +
							  " = " + p.description() );
					pw.print( p.start() + "|" + p.length() +
							  "|" + p.description() );
					if( p.isAllocated() ) {
						pw.println();
						continue;
					}
					byte[] hash = digest( p );
					String hashHex = new String( Hex.encodeHex( hash ) );
					pw.println( "|" + hashHex );

				}
				if( addAttribute ) {
					String value = sw.toString();
					String key = "hashvolsys";
					store.setAttribute( mdd, key, value.getBytes() );
				}
			} finally {
				// MUST release vs else leaves mdfs non-unmountable
				vs.close();
			}
		} finally {
			// MUST release i else leaves mdfs non-unmountable
			i.close();
		}
	}

	private byte[] digest( Partition p ) throws IOException {
		MD.reset();
		try( InputStream is = p.getInputStream() ) {
				DigestInputStream dis = new DigestInputStream( is, MD );
				while( true ) {
					int nin = dis.read( DIGESTBUFFER );
					if( nin < 0 )
						break;
				}
			}
		return MD.digest();
	}

	String diskID, sessionID;
	static boolean verbose;
	boolean addAttribute;

	static byte[] DIGESTBUFFER = new byte[ 1024*1024 ];
	static MessageDigest MD = null;
	static {
		try {
			MD = MessageDigest.getInstance( "sha1" );
		} catch( NoSuchAlgorithmException never ) {
		}
	}
}

// eof
