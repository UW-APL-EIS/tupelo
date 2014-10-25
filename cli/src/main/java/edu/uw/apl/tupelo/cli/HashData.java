package edu.uw.apl.tupelo.cli;

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

public class HashData {

	static public void main( String[] args ) {
		HashData main = new HashData();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			if( debug )
				e.printStackTrace();
			System.exit(-1);
		}
	}

	public HashData() {
		storeLocation = Utils.STORELOCATIONDEFAULT;
	}

	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	public void readArgs( String[] args ) {
		Options os = Utils.commonOptions();
		os.addOption( "d", false, "Debug" );
		os.addOption( "v", false, "Verbose" );

		final String USAGE =
			HashData.class.getName() +
			" [-d] [-v] [-s storeLocation] diskID sessionID";
		final String HEADER = "";
		final String FOOTER = "";
		
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		debug = cl.hasOption( "d" );
		verbose = cl.hasOption( "v" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
		args = cl.getArgs();
		if( args.length < 2 ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
	}
	
	public void start() throws Exception {

		Store store = Utils.buildStore( storeLocation );
		if( debug )
			System.out.println( "Store type: " + store );

		Collection<ManagedDiskDescriptor> stored = store.enumerate();
		System.out.println( "Stored: " + stored );

		ManagedDiskDescriptor mdd = Utils.locateDescriptor( store, diskID,
															sessionID );
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
		Thread.sleep( 1000 * 2 );

		File f = mdfs.pathTo( mdd );
		System.out.println( "Located Managed Data: " + f );
		Image i = new Image( f );
		VolumeSystem vs = null;
		try {
			vs = new VolumeSystem( i );
		} catch( IllegalArgumentException iae ) {
			// MUST release i else leaves mfs non-unmountable
			i.close();
			System.err.println( "No volume system on " + f );
			System.exit( 1 );
		}
		
		//		Thread.sleep( 1000 * 10 );
		List<Partition> ps = vs.getPartitions();
		for( Partition p : ps ) {
			if( !p.isAllocated() )
				continue;
			System.out.println( "At sector " + p.start() +
								", located " + p.description() );
			Map<String,byte[]> fileHashes = new HashMap<String,byte[]>();
			walk( i, p.start(), fileHashes );
			System.out.println( " FileHashes : " + fileHashes.size() );
			record( fileHashes, p.start(), p.length() );
		}
		vs.close();
		i.close();
	}
	
	private void walk( Image i, long start,
					   final Map<String,byte[]> fileHashes )
		throws Exception {
		FileSystem fs = new FileSystem( i, start );
		DirectoryWalk.Callback cb = new DirectoryWalk.Callback() {
				public int apply( WalkFile f, String path ) {
					try {
						process( f, path, fileHashes );
						return Walk.WALK_CONT;
					} catch( Exception e ) {
						System.err.println( e );
						return Walk.WALK_ERROR;
					}
				}
			};
		int flags = DirectoryWalk.FLAG_NONE;
		// LOOK: visit deleted files too ??
		flags |= DirectoryWalk.FLAG_ALLOC;
		flags |= DirectoryWalk.FLAG_RECURSE;
		flags |= DirectoryWalk.FLAG_NOORPHAN;
		fs.dirWalk( fs.rootINum(), flags, cb );
		fs.close();
	}

	private void process( WalkFile f, String path,
						  Map<String,byte[]> fileHashes )
		throws IOException {

		String name = f.getName();
		if( name == null )
			return;
		if(	"..".equals( name ) || ".".equals( name ) ) {
			return;
		}
		Meta m = f.meta();
		if( m == null )
			return;
		// LOOK: hash directories too ??
		if( m.type() != Meta.TYPE_REG )
			return;
		Attribute defa = f.getAttribute();
		// Seen some weirdness where an allocated file has no attribute(s) ??
		if( defa == null )
			return;

		if( debug )
			System.out.println( path + "/" + name );

		String wholeName = path + "/" + name;
		byte[] digest = digest( defa );
		fileHashes.put( wholeName, digest );
	}
	
	private byte[] digest( Attribute a ) throws IOException {
		MD5.reset();
		InputStream is = a.getInputStream();
		DigestInputStream dis = new DigestInputStream( is, MD5 );
		while( true ) {
			int nin = dis.read( DIGESTBUFFER );
			if( nin < 0 )
				break;
		}
		return MD5.digest();
	}
	
	private void record( Map<String,byte[]> fileHashes,
						 long start, long length )
		throws Exception {

		List<String> sorted = new ArrayList<String>( fileHashes.keySet() );
		Collections.sort( sorted );
		String outName = diskID + "-" + sessionID + "-" +
			start + "-" + length + ".md5";
		System.out.println( "Writing: " + outName );
		
		FileWriter fw = new FileWriter( outName );
		BufferedWriter bw = new BufferedWriter( fw, 1024 * 1024 );
		PrintWriter pw = new PrintWriter( bw );
		for( String fName : sorted ) {
			byte[] hash = fileHashes.get( fName );
			String s = new String( Hex.encodeHex( hash ) );
			pw.println( s + " " + fName );
		}
		pw.close();
	}
	

	String storeLocation;
	String diskID, sessionID;
	static boolean debug, verbose;

	static byte[] DIGESTBUFFER = new byte[ 1024*1024 ];
	static MessageDigest MD5 = null;
	static {
		try {
			MD5 = MessageDigest.getInstance( "md5" );
		} catch( NoSuchAlgorithmException never ) {
		}
	}
	}

// eof
