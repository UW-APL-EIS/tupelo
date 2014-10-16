package edu.uw.apl.tupelo.store;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.commons.io.FileUtils;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

/**
   An implementation of the Tupelo Store interface which uses a flat
   file system for Store component management.  Uses a local directory
   heirarchy for managed disks and attribute management.

   Note that the Store interface has no description of any
   synchronisation constraints.  That is left to us here.  Given that
   this FSStore object may be used by our webapp/server war, we need
   to pay attention to multi-threaded access.  To do this, we maintain
   a single lock, but use it more sparingly than at every method
   interface.  Used that way, a single expensive put blocks
   every other operation.  Instead, we still use the native lock on
   the FSStore object, but reduce the critical region size where
   possible.  In particular, a put first writes to a temp dir,
   and only when it is time to move into the final Store location do
   we require the lock.
*/

public class FilesystemStore implements Store {

	public FilesystemStore( File root ) {
		log = Logger.getLogger( getClass() );
		this.root = root;
		log.info( "FSStore.root = " + root );
		tempDir = new File( root, "temp" );
		tempDir.mkdirs();
		log.info( "FSStore.tmp = " + tempDir );
		fuseDir = new File( root, "fuse" );
		fuseDir.mkdirs();
		log.info( "FSStore.fuse = " + fuseDir );
		uuid = loadUUID();
		diskMap = new HashMap<String,ManagedDisk>();
		loadManagedDisks();
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
				ManagedDisk md = ManagedDisk.load( f );
				ManagedDiskDescriptor mdd = md.getDescriptor();
				String path = asPathName( mdd );
				diskMap.put( path, md );
				log.info( "Loaded managed disk: " + f );
			} catch( IOException ioe ) {
				log.warn( ioe );
				continue;
			}
		}
		Collection<ManagedDisk> allDisks = diskMap.values();
		for( ManagedDisk md : allDisks ) {
			// LOOK: withdraw any ManagedDisk which cannot be fully linked
			link( md, allDisks );
		}
	}

	private void link( ManagedDisk md, Collection<ManagedDisk> allDisks ) {
		if( !md.hasParent() )
			return;
		UUID linkage = md.getUUIDParent();
		ManagedDisk parent = locate( linkage, allDisks );
		md.setParent( parent );
		link( parent, allDisks );
	}

	private ManagedDisk locate( UUID needle, Collection<ManagedDisk> allDisks ) {
		for( ManagedDisk md : allDisks ) {
			if( md.getUUIDCreate().equals( needle ) )
				return md;
		}
		throw new IllegalStateException( "No such uuid: " + needle );
	}

	static String asPathName( ManagedDiskDescriptor mdd ) {
		return mdd.getDiskID() + File.separator +
			mdd.getSession().toString();
	}


	private final UUID uuid;
	private final File root, tempDir, fuseDir;
	private final Map<String,ManagedDisk> diskMap;
	private final Logger log;
}

// eof

