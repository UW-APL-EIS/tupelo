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
import edu.uw.apl.commons.sleuthkit.filesys.FileSystem;
import edu.uw.apl.commons.sleuthkit.volsys.Partition;
import edu.uw.apl.commons.sleuthkit.volsys.VolumeSystem;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileBuilder;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileCodec;

/**
 * Note: This program makes use of fuse4j/fuse and so has an impact on
 * the host filesystem as a whole.  In principle, a fuse mount point is
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

public class BodyFile extends Base {

	static public void main( String[] args ) {
		BodyFile main = new BodyFile();
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

	public BodyFile() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "a", false,
					  "Hash all managed disks (those done not re-computed)" );
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
		all = cl.hasOption( "a" );
		verbose = cl.hasOption( "v" );
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
	
	public void start() throws Exception {
		File dir = new File( storeLocation );
		if( !dir.isDirectory() ) {
			throw new IllegalStateException
				( "Not a directory: " + storeLocation );
		}
		store = new FilesystemStore( dir );
		if( debug )
			System.out.println( "Store type: " + store );

		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		
		final File mountPoint = new File( "test-mount" );
		if( !mountPoint.exists() ) {
			mountPoint.mkdirs();
			mountPoint.deleteOnExit();
		}
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
		
		if( all ) {
			Collection<ManagedDiskDescriptor> mdds = store.enumerate();
			for( ManagedDiskDescriptor mdd : mdds ) {
				File f = mdfs.pathTo( mdd );
				bodyFiles( f, mdd );
			}
		} else {
			ManagedDiskDescriptor mdd = locateDescriptor( store,
														  diskID, sessionID );
			if( mdd == null ) {
				System.err.println( "Not stored: " + diskID + "," +
									sessionID );
				System.exit(1);
			}
			File f = mdfs.pathTo( mdd );
			bodyFiles( f, mdd );
		}
	}

	private void bodyFiles( File f, ManagedDiskDescriptor mdd )
		throws Exception {
		Image i = new Image( f );
		try {
			VolumeSystem vs = null;
			try {
				vs = new VolumeSystem( i );
			} catch( IllegalStateException noVolSys ) {
				log.warn( noVolSys );
				return;
			}
			try {
				List<Partition> ps = vs.getPartitions();
				for( Partition p : ps ) {
					if( !p.isAllocated() ) {
						continue;
					}
					FileSystem fs = null;
					try {
						log.info( p.start() + " " +
								  p.length() + " " +
								  p.description() );
						fs = new FileSystem( i, p.start() );
						edu.uw.apl.commons.sleuthkit.digests.BodyFile bf =
							BodyFileBuilder.create( fs );
						StringWriter sw = new StringWriter();
						BodyFileCodec.format( bf, sw );
						String value = sw.toString();
						String key = "bodyfile-" +
							p.start() + "-" + p.length();
						store.setAttribute( mdd, key, value.getBytes() );
						//						BodyFileCodec.format( bf, System.out );
						fs.close();
					} catch( IllegalStateException noFileSystem ) {
						continue;
					}
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

	
	String diskID, sessionID;
	static boolean verbose;
	FilesystemStore store;
	boolean all;
}

// eof
