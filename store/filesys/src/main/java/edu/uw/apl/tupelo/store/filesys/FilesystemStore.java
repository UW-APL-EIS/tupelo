package edu.uw.apl.tupelo.store.filesys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.ProgressMonitor;
import edu.uw.apl.tupelo.store.Store;

/**
   An implementation of the Tupelo Store interface which uses a flat
   file system for Store component management.  Uses a local directory
   heirarchy for managed disks and attributes.

   Note that the Store interface has no description of any
   synchronisation constraints.  That is left to us here.  Given that
   this FSStore object may be used by a webapp/server war, we need to
   pay attention to multi-threaded access.  To do this, we maintain a
   single lock, but use it more sparingly than at every method
   boundary.  Used that way, a single expensive put() blocks every
   other operation.  Instead, we still use the native lock on the
   FSStore object, but reduce the critical region size where possible.
   In particular, a put() first writes to a temp dir/file, and only
   when it is time to move into the final Store location do we require
   the 'real' lock.
*/

public class FilesystemStore implements Store {

	public FilesystemStore( File root ) {
		this( root, true );
	}

	/**
	 * @param root - root directory for the Store. ALL data is held
	 * under this single root.
	 *
	 * @param loadManagedDisks - normally true, but for test cases useful to
	 * pass false so a FilesystemStore has known empty status initially
	 */
	public FilesystemStore( File root, boolean loadManagedDisks ) {
		log = LogFactory.getLog( getClass() );
		this.root = root;
		log.info( "Store.root = " + root );
		tempDir = new File( root, "temp" );
		tempDir.mkdirs();
		log.debug( "FSStore.tmp = " + tempDir );
		uuid = loadUUID();
		descriptorMap = new HashMap<ManagedDiskDescriptor,ManagedDisk>();
		pathMap = new HashMap<String,ManagedDisk>();
		if( loadManagedDisks )
			loadManagedDisks();
		writable = true;
	}

	/**
	 * To protect assets in the store, we can make dirs un-writable. Default
	 * is writable, so in testing the user can wipe the store directory
	 */
	public void setWritable( boolean b ) {
		writable = b;
	}
	
	@Override
	public synchronized UUID getUUID() {
		return uuid;
	}

	@Override
	public synchronized long getUsableSpace() {
		return root.getUsableSpace();
	}

	/**
	   a new session is derived from an earlier one persisted to disk,
	   or entirely new if no persistent one available.  We persist the
	   result back to disk for future usage.  So the disk file
	   'session.txt' is part of the Store state.
	*/
	@Override
	public synchronized Session newSession() throws IOException {
		Calendar now = Calendar.getInstance( Session.UTC );
		Session result = null;
		File f = new File( root, "session.txt" );
		if( f.exists() ) {
			BufferedReader br = new BufferedReader( new FileReader( f ) );
			String line = br.readLine();
			try {
				Session saved = Session.parse( line );
				result = saved.successor( now );
			} catch( ParseException pe ) {
				log.warn( pe );
				result = new Session( uuid, now, 1 );
			}
		} else {
			result = new Session( uuid, now, 1 );
		}
		PrintWriter pw = new PrintWriter( new FileWriter( f ) );
		pw.println( result.format() );
		pw.close();
		return result;
	}

	@Override
	public synchronized void put( ManagedDisk md ) throws IOException {

		ManagedDiskDescriptor mdd = md.getDescriptor();
		if( descriptorMap.containsKey( mdd ) )
			throw new IllegalArgumentException( "Already stored: " + mdd );
		
		String fileName = asFileName( mdd );
		File tempFile = new File( tempDir, fileName );
		log.info( "Writing to " + tempFile );
		/*
		  Since the temp file itself is to be used as a lock,
		  canonicalise it first to avoid any unintended side-stepping
		  of lock requirements.  Since this is where the expensive
		  operation occurs, we maintain the accessibility of the wider
		  store object itself...
		*/
		tempFile = tempFile.getCanonicalFile();
		synchronized( tempFile ) {
			log.debug( "Locked " + tempFile );
			FileOutputStream fos = new FileOutputStream( tempFile );
			BufferedOutputStream bos = new BufferedOutputStream( fos, 1024*1024*16 );
			md.writeTo( bos );
			bos.close();
			fos.close();
			log.debug( "Unlocked " + tempFile );
		}

		// we are now adding to the Store proper, so need the lock....
		synchronized( this ) {
			File outDir = diskDataDir( root, mdd );
			outDir.mkdirs();
			File outFile = new File( outDir, fileName );
			log.info( "Moving to " + outFile );
			tempFile.renameTo( outFile );
			log.info( "Moved to " + outFile );
			md.setManagedData( outFile );

			// Access controls in place to guard against file system screw ups..
			outFile.setWritable( writable );
			// Since only ever supposed to be a single file in the dir, protect the dir
			outDir.setWritable( writable );
			
			// LOOK: link to parent ??
			descriptorMap.put( mdd, md );
			String path = asPathName( mdd );
			pathMap.put( path, md );
		}
	}

	@Override
	public synchronized void put( ManagedDisk md, ProgressMonitor pm )
		throws IOException {

		// TODO
	}
	

		/**
	 * Produce a sha1 hash of each grain in the managed disk
	 * identified by the supplied descriptor.
	 
	 * Asserting that we do NOT need this to be synchronized,
	 * since we are reading data already managed, and by definition
	 * that cannot change.

	 * LOOK: We may need to stream this out instead of holding it all.
	 * A 2TB disk using 16 million grains and a hash of 20bytes per grain
	 * means 320MB of memory!
	 *
	 * LOOK: We may/should have this already held as an attribute!
	 */
	@Override
	public List<byte[]> digest( ManagedDiskDescriptor mdd )
		throws IOException {

		File f = managedDataFile( root, mdd );
		if( !f.isFile() )
			return Collections.emptyList();

		List<byte[]> result = new ArrayList<byte[]>();
		ManagedDisk md = ManagedDisk.readFrom( f );
		MessageDigest sha1 = null;
		try {
			sha1 = MessageDigest.getInstance( "sha1" );
		} catch( NoSuchAlgorithmException never ) {
		}

		byte[] grain = new byte[(int)md.grainSizeBytes()];
		InputStream is = md.getInputStream();
		DigestInputStream dis = new DigestInputStream( is, sha1 );
		while( true ) {
			int nin = dis.read( grain );
			if( nin == -1 )
				break;
			if( nin != grain.length ) {
				throw new IllegalStateException( "Partial read. Fix!" );
			}
			byte[] hash = sha1.digest();
			result.add( hash );
			sha1.reset();
		}
		dis.close();
		is.close();

		// Sanity check ??  SHOULD THROW ERROR ???
		if( result.size() != md.size() / md.grainSizeBytes() ) {
			log.warn( "Unexpected digest length: " + result.size() +
					  "/" + (md.size() / md.grainSizeBytes()) );
			throw new IllegalStateException( "Bad digest compute: " + mdd );
		}
		
		return result;
	}
	
	// for the benefit of the fuse-based ManagedDiskFileSystem
	@Override
	public ManagedDisk locate( ManagedDiskDescriptor mdd ) {
		return descriptorMap.get( mdd );
	}

	@Override
	public synchronized Collection<ManagedDiskDescriptor> enumerate()
		throws IOException {
		return descriptorMap.keySet();
	}

	@Override
	public Collection<String> listAttributes( ManagedDiskDescriptor mdd )
		throws IOException {
		File dir = attrDir( root, mdd );
		if( !dir.isDirectory() )
			return Collections.emptyList();
		List<String> result = new ArrayList<String>();
		File[] fs = dir.listFiles();
		for( File f : fs ) {
			result.add( f.getName() );
		}
		return result;
	}

	@Override
	public void setAttribute( ManagedDiskDescriptor mdd,
							  String key, byte[] value ) throws IOException {

		String fileName = key;
		File outDir = attrDir( root, mdd );
		outDir.mkdirs();
		File outFile = new File( outDir, fileName );
		outFile = outFile.getCanonicalFile();
		synchronized( outFile ) {
			log.debug( "Locked " + outFile );
			FileUtils.writeByteArrayToFile( outFile, value );
			log.debug( "Unlocked " + outFile );
		}
	}

	@Override
	public byte[] getAttribute( ManagedDiskDescriptor mdd, String key )
		throws IOException {
		String fileName = key;
		File dir = attrDir( root, mdd );
		if( !dir.isDirectory() )
			return null;
		File inFile = new File( dir, key );
		if( !inFile.isFile() )
			return null;
		return FileUtils.readFileToByteArray( inFile );
	}
	
	/*********************** Private Implementation *********************/
	
	private UUID loadUUID() {
		UUID result = null;
		File f = new File( root, "uuid.txt" );
		if( f.exists() ) {
			String line = null;
			try {
				BufferedReader br = new BufferedReader( new FileReader( f ) );
				line = br.readLine();
				br.close();
			} catch( IOException ioe ) {
				log.warn( ioe );
				throw new IllegalStateException( "UUID read error!" );
			}
			try {
				result = UUID.fromString( line );
			} catch( IllegalArgumentException iae ) {
				log.warn( iae );
				throw new IllegalStateException( "UUID parse error!" );
			}
		} else {
			result = UUID.randomUUID();
			try {
				PrintWriter pw = new PrintWriter( new FileWriter( f ) );
				pw.println( result );
				pw.close();
			} catch( IOException ioe ) {
				log.warn( ioe );
				throw new IllegalStateException( "UUID write error!" );
			}
		}
		return result;
	}

	private void loadManagedDisks() {
		File dir = new File( root, "disks" );
		dir.mkdirs();
		Collection<File> fs = FileUtils.listFiles
			( dir, new String[] { ManagedDisk.FILESUFFIX.substring(1) }, true );
		for( File f : fs ) {
			try {
				ManagedDisk md = ManagedDisk.readFrom( f );
				ManagedDiskDescriptor mdd = md.getDescriptor();
				descriptorMap.put( mdd, md );
				String path = asPathName( mdd );
				pathMap.put( path, md );
				log.debug( "Located managed disk: " + f );
			} catch( IOException ioe ) {
				log.warn( ioe );
				continue;
			}
		}
		Collection<ManagedDisk> allDisks = pathMap.values();
		List<ManagedDisk> linkedDisks = new ArrayList<ManagedDisk>
			( allDisks.size() );
		for( ManagedDisk md : allDisks ) {
			// LOOK: withdraw any ManagedDisk which cannot be fully linked
			link( md, allDisks, linkedDisks );
		}
	}

	/**
	 * @param linkedDisks - the accumlating result, often needed for
	 * base case testing when using a recursive process
	 */
	private void link( ManagedDisk md, Collection<ManagedDisk> allDisks,
					   List<ManagedDisk> linkedDisks ) {
		if( !md.hasParent() )
			return;
		if( linkedDisks.contains( md ) )
			return;
		UUID linkage = md.getUUIDParent();
		ManagedDisk parent = locate( linkage, allDisks );
		md.setParent( parent );
		linkedDisks.add( md );
		link( parent, allDisks, linkedDisks );
	}

	private ManagedDisk locate( UUID needle,
								Collection<ManagedDisk> allDisks ) {
		for( ManagedDisk md : allDisks ) {
			if( md.getUUIDCreate().equals( needle ) )
				return md;
		}
		throw new IllegalStateException( "No such uuid: " + needle );
	}

	/*
	  The file sys layout here is

	  disks/
	  disks/DISKID/SESSIONID
	  disks/DISKID/SESSIONID/data
	  disks/DISKID/SESSIONID/data/DISKID-SESSIONID.tmd
	  disks/DISKID/SESSIONID/attrs
	  disks/DISKID/SESSIONID/attrs/SOMEATTR
	*/
	static private File diskDir( File root, ManagedDiskDescriptor vd ) {
		File dir = new File( root, "disks" );
		dir = new File( dir, vd.getDiskID() );
		dir = new File( dir, vd.getSession().toString() );
		return dir;
	}

	static private File diskDataDir( File root, ManagedDiskDescriptor mdd ) {
		File dir = diskDir( root, mdd );
		dir = new File( dir, "data" );
		return dir;
	}

	static private File managedDataFile( File root,
										 ManagedDiskDescriptor mdd ) {
		File dir = diskDataDir( root, mdd );
		File result = new File( dir, asFileName( mdd ) );
		return result;
	}

	static private File attrDir( File root, ManagedDiskDescriptor mdd ) {
		File dir = diskDir( root, mdd );
		dir = new File( dir, "attrs" );
		return dir;
	}

	static String asPathName( ManagedDiskDescriptor mdd ) {
		return mdd.getDiskID() + File.separator +
			mdd.getSession().toString();
	}

	static String asFileName( ManagedDiskDescriptor mdd ) {
		return mdd.getDiskID() + "-" +
			mdd.getSession().toString() + ManagedDisk.FILESUFFIX;
	}

	private boolean writable;
	
	private final UUID uuid;
	private final File root, tempDir;
	private final Map<ManagedDiskDescriptor,ManagedDisk> descriptorMap;
	private final Map<String,ManagedDisk> pathMap;
	private final Log log;
}

// eof

