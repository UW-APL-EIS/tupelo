package edu.uw.apl.tupelo.store.tools;

import java.io.File;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

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
 * the HOST filesystem as a whole.  In principle, a fuse mount point
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

public class HashFS {

	static public class Main extends MDFSBase {
		static public void main( String[] args ) {
			Main main = new Main();
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
		
		@Override
		protected void process( File mdfsPath, ManagedDiskDescriptor mdd )
			throws Exception {

			/*
			  Delegate to HashFS class, which has the core logic.
			  This way, we can have many entries points into that core logic,
			  e.g. http/server calls too.
			*/
			HashFS.process( mdfsPath, mdd, store );
		}
	}

	static public void process( File mdfsPath, ManagedDiskDescriptor mdd,
								Store store )
		throws Exception {
		
		Image i = new Image( mdfsPath );
		try {
			log.debug( "Trying volume system on " + mdfsPath );
			boolean b = walkVolumeSystem( i, mdd, store );
			if( !b ) {
				log.debug( "Trying file system on " + mdfsPath );
				walkFileSystem( i, mdd, store );
			}
		} finally {
			i.close();
		}
	}

	/**
	 * @return true if a volume system found (and thus traversed), false
	 * otherwise.  False result lets us try the image as a standalone
	 * filesystem
	 */
	static private boolean walkVolumeSystem
		( Image i, ManagedDiskDescriptor mdd, Store store )
		throws Exception {

		VolumeSystem vs = null;
		try {
			vs = new VolumeSystem( i );
		} catch( Exception iae ) {
			return false;
		}
		List<Partition> ps = vs.getPartitions();
		try {
			for( Partition p : ps ) {
				if( !p.isAllocated() )
					continue;
				log.debug( "At sector " + p.start() +
						   ", located " + p.description() );
				String key = keyName( p.start(), p.length() );
				boolean exists = Utils.isAttributePresent( store, mdd, key );
				if( exists )
					continue;
				Map<String,byte[]> fileHashes = new HashMap<String,byte[]>();
				FileSystem fs = null;
				try {
					fs = new FileSystem( i, p.start() );
					walk( fs, fileHashes );
					fs.close();
					log.info( " FileHashes : " + fileHashes.size() );
					record( fileHashes, p.start(), p.length(), mdd, store );
				} catch( IllegalStateException noFilesystem ) {
					continue;
				}
			}
		} finally {
			// MUST release vs else leaves mdfs non-unmountable (??)
			vs.close();
		}
		return true;
	}

	static private void walkFileSystem( Image i, ManagedDiskDescriptor mdd,
										Store store )
		throws Exception {

		String key = keyName( 0, 0 );
		boolean exists = Utils.isAttributePresent( store, mdd, key );
		if( exists )
			return;
		
		Map<String,byte[]> fileHashes = new HashMap<String,byte[]>();
		FileSystem fs = new FileSystem( i );
		try {
			walk( fs, fileHashes );
			log.debug( "FileHashes: " + fileHashes.size() );
			// signify a standalone file system via a 0,0 sector interval
			record( fileHashes, 0, 0, mdd, store );
		} finally {
			fs.close();
		}
	}
	
	static private void walk( FileSystem fs,
							  final Map<String,byte[]> fileHashes )
		throws Exception {
		DirectoryWalk.Callback cb = new DirectoryWalk.Callback() {
				public int apply( WalkFile f, String path ) {
					try {
						process( f, path, fileHashes );
						return Walk.WALK_CONT;
					} catch( Exception e ) {
						log.warn( e );
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

	static private void process( WalkFile f, String path,
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

		/*
		  if( debug )
			System.out.println( "'" + path + "' '" + name + "'" );
		*/
		
		String wholeName = path + name;
		byte[] digest = digest( defa );
		fileHashes.put( wholeName, digest );
	}
	
	static private byte[] digest( Attribute a ) throws IOException {
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

	static String keyName( long startSector, long sectorCount ) {
		return "hashfs-" + startSector + "-" + sectorCount;
	}
	
	static private void record( Map<String,byte[]> fileHashes,
								long start, long length,
								ManagedDiskDescriptor mdd,
								Store store )
		throws Exception {
		
		List<String> sorted = new ArrayList<String>( fileHashes.keySet() );
		Collections.sort( sorted );
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		for( String fName : sorted ) {
			byte[] hash = fileHashes.get( fName );
			String s = new String( Hex.encodeHex( hash ) );
			pw.println( s + " " + fName );
		}
		pw.close();
		String value = sw.toString();
		String key = keyName( start, length );
		store.setAttribute( mdd, key, value.getBytes() );
	}
	
	static byte[] DIGESTBUFFER = new byte[ 1024*1024 ];
	static MessageDigest MD5 = null;
	static {
		try {
			MD5 = MessageDigest.getInstance( "md5" );
		} catch( NoSuchAlgorithmException never ) {
		}
	}

	static private final Log log = LogFactory.getLog( HashFS.class );
}

// eof
