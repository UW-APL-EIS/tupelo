package edu.uw.apl.tupelo.fuse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.BufferOverflowException;

import fuse.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.SeekableInputStream;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

/**
 * Expose the contents of a Tupelo store as a <em>filesystem</em>,
 * suitable for inspection by e.g. <code>md5sum, dd, mmls, fls,
 * etc</code>.  Each file exposed is a <em>device file</em> rather
 * than a regular file, so think <code>/dev/sda</code> and not
 * <code>/etc/fstab</code>. Only device-aware tools like the ones
 * mentioned above can make any sense of this filesystem.

 * Typical usage is
 * <pre>
 *
 * Store s = new FilesystemStore();
 * ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( s );
 * File mountPoint = new File( "mount" );
 * mountPoint.mkdirs();
 * boolean ownThread = true|false;
 * mdfs.mount( mountPoint, ownThread );
 * </pre>
 *
 * where <code>ownThread</code> depends on the calling code needing to
 * continue or not.  When used in a main program (see e.g. {@link
 * Main}), set to false.  When used in e.g. a web application, set to
 * true.
 * <p>
 * To unmount, externally run {@code fusermount -u mount}.  This
 * same command could also be wrapped in a
 * <code>Process/ProcessBuilder</code> if to be run locally, which is
 * how the {@link #umount() umount} method works.
 *
 * <p>
 * The filessytem layout for the managed disks is then
 *
 * <pre>
 * /diskIDX/sessionIDY
 * </pre>
 *
 * for all disks X and all sessions Y.  You could then do this, for
 * some available file, <p>
 *
 * <pre>
 * $ md5sum /path/to/mount/diskID1/sessionID2
 * </pre>
 *
 * Note how we access the 'Store' object totally by the base Store
 * interface.  We do NOT need to know here HOW the Store is
 * implemented (though of course the likely implementation is a
 * FilesystemStore).
 */
public class ManagedDiskFileSystem extends AbstractFilesystem3 {

	/**
	 * Construct a ManagedDiskFileSystem given a Tupelo store.  Will
	 * expose each managed disk (a what/when pair) as a device file
	 * under the ManagedDiskFileSystem mount point.
	 *
	 @param s the Tupelo store whose managed disks are to exposed as a
	 filesystem.
	 */
	public ManagedDiskFileSystem( Store s ) {
		store = s;
		startTime = (int) (System.currentTimeMillis() / 1000L);
		readBuffers = new HashMap<Object,byte[]>();

		log = LogFactory.getLog( getClass() );

		try {
			Collection<ManagedDiskDescriptor> mdds = store.enumerate();
			for( ManagedDiskDescriptor mdd : mdds ) {
				log.info( "Exposing: " + mdd.getDiskID() +
						  "/" + mdd.getSession() );
			}
		} catch( IOException ioe ) {
			log.warn( ioe );
		}
	}
	
	/**
	 * Do the fuse mount.  Until this command called, the store's
	 * contents are not visible to the host filesystem.
	 *
	 * @param mountPoint a directory on the host file system at which
	 * to do the mount.  Must exist a priori.

	 * @param ownThread false if caller willing to block until the
	 * mount is torn down (by an external <code>fusermount -u
	 * mount</code>).  A caller which needs a new thread spawned
	 * supplies true.
	 */
	public void mount( File mountPoint, boolean ownThread ) throws Exception {
		if( !mountPoint.isDirectory() )
			throw new IllegalArgumentException( "Mountpoint not a dir: " +
												mountPoint );
		this.mountPoint = mountPoint;
		/*
		  The -f says no fork, we need this!!

		  The -s says single-threaded, we need this!!

		  LOOK: Consider relaxing this and synchronizing on the
		  SeekableInputStream objects, may improve performance ??
		  Can't have arbitrary multi-threaded access to each
		  SeekableInputStream (since it has state and is NOT
		  synchronized internally) but MT-access to distinct
		  SeekableInputStreams likely OK. Likely we are not achieving
		  this latter situation even with the current -s option set to
		  true.  Not sure if 'fuse single threaded' means a single
		  thread for the entire filesystem, or a single thread per
		  opened file.
		  
		  The -r says read-only, which makes sense here
		*/
		String[] args = { mountPoint.getPath(), "-f", "-s", "-r"  };

		/*
		  If we supply the fuse package OUR logger, we cannot separate
		  out logging by package name, like we usually do. So create
		  two loggers, one for US and one for THEM
		*/
		Log logFuse = LogFactory.getLog( "fuse" );
		if( ownThread ) {
			ThreadGroup tg = new ThreadGroup( "MDFS.Threads" );
			FuseMount.mount( args, this, tg, logFuse );
		} else {
			FuseMount.mount( args, this, logFuse );
		}
	}

	/**
	 * Unmount the filesystem, the dual of {@link #mount(java.io.File,
	 * boolean) mount}.  Only makes sense if the mount was done such
	 * that the caller was able to continue, with the file system
	 * running in its own thread.
	 *
	 * @return The exit code of the 'fusermount -u' sub-process
	 */
	public int umount() throws Exception {
		String cmdLine = "fusermount -u " + mountPoint;
		log.info( "Execing: " + cmdLine );
		//		Process p = Runtime.getRuntime().exec( cmdLine );
		ProcessBuilder pb = new ProcessBuilder( "fusermount", "-u",
												mountPoint.toString() );
		pb.redirectErrorStream( true );
		pb.redirectOutput( new File( "mdfs.pb" ) );
		Process p = pb.start();
		p.waitFor();
		log.info( "Result: " + p.exitValue() );
		return p.exitValue();
	}

	// LOOK: how do we handle io errors from store?
	private Collection<ManagedDiskDescriptor> descriptors() {
		try {
			return store.enumerate();
		} catch( IOException ioe ) {
			log.warn( ioe );
			return Collections.emptyList();
		}
	}

	/**
	   Convenience method, for applications to derive where in the
	   mounted file system a ManagedDisk can be located.

	   @param mdd a descriptor (diskID+sessionID) for the managed disk
	   of interest
	*/
	public File pathTo( ManagedDiskDescriptor mdd ) {
		File f = new File( mountPoint, mdd.getDiskID() );
		f = new File( f, mdd.getSession().toString() );
		return f;
	}
	
	@Override
	public int getattr( String path, FuseGetattrSetter getattrSetter )
		throws FuseException {
		if( log.isTraceEnabled() ) {
			log.trace( "getattr " + path );
		}

		Collection<ManagedDiskDescriptor> mdds = descriptors();

		if( path.equals( "/" ) ) {
			int count = mdds.size();
			int time = startTime;
			getattrSetter.set
				( path.hashCode(), FuseFtypeConstants.TYPE_DIR | 0755, 2,
				  0, 0, 0,
				  // size, blocks lifted from example FakeFilesystem...
				  count * 128, (count * 128 + 512 - 1) / 512,
				  time, time, time);
			return 0;
		}
		
		String details = path.substring(1);
		Matcher m1 = DISKIDPATHREGEX.matcher( details );
		if( m1.matches() ) {
			String diskID = m1.group(1);
			List<ManagedDiskDescriptor> matching =
				new ArrayList<ManagedDiskDescriptor>();
			for( ManagedDiskDescriptor mdd : mdds ) {
				if( mdd.getDiskID().equals( diskID ) ) {
					matching.add( mdd );
				}
			}
			if( matching.isEmpty() )
				return Errno.ENOENT;
			int count = matching.size();
			int time = startTime;
			getattrSetter.set
				( path.hashCode(),
				  FuseFtypeConstants.TYPE_DIR | 0755, 2,
				 0, 0, 0,
				 // size, blocks lifted from example FakeFilesystem...
				 count * 128, (count * 128 + 512 - 1) / 512,
				 time, time, time);
			return 0;
		}
		
		Matcher m2 = MANAGEDDISKDESCRIPTORPATHREGEX.matcher( details );
		if( m2.matches() ) {
			//System.out.println( "getAttr(VD)." + m.group(0) );
			String diskID = m2.group(1);
			String sessionID = m2.group(2);
			ManagedDiskDescriptor matching = null;
			for( ManagedDiskDescriptor mdd : mdds ) {
				if( mdd.getDiskID().equals( diskID ) &&
					mdd.getSession().toString().equals( sessionID ) ) {
					/*
					  System.out.println( "getattr.Matched: " + mdd +
										" to " + MANAGEDDISKDESCRIPTORPATHREGEX );
					*/
					matching = mdd;
					break;
				}
			}
			if( matching == null )
				return Errno.ENOENT;

			int time = startTime;// LOOK: link to session date/time?
			ManagedDisk md = store.locate( matching );
			long size = md.size();
			getattrSetter.set
				( matching.hashCode(), FuseFtypeConstants.TYPE_FILE | 0444,
				  1, 0, 0, 0, size, (size + 512 - 1) / 512,
				  time, time, time );
			return 0;
		}

		return Errno.ENOENT;
	}

	@Override
	public int getdir(String path, FuseDirFiller filler )
		throws FuseException {

		Collection<ManagedDiskDescriptor> mdds = descriptors();

		if( log.isTraceEnabled() )
			log.trace( "getdir: " + path );

		//		System.out.println( "getdir: " + path );

		if( "/".equals( path ) ) {
			Set<String> matching = new HashSet<String>();
			for( ManagedDiskDescriptor mdd : mdds ) {
				matching.add( mdd.getDiskID() );
			}
			for( String s : matching ) {
				filler.add( s, s.hashCode(),
							FuseFtypeConstants.TYPE_DIR | 0755 );
			}
			return 0;
		}
		String details = path.substring(1);
		Matcher m1 = DISKIDPATHREGEX.matcher( details );
		if( m1.matches() ) {
			String needle = m1.group(0);
			List<String> matchingDirs = new ArrayList<String>();
			for( ManagedDiskDescriptor mdd : mdds ) {
				//				System.out.println( mdd );
				if( mdd.getDiskID().equals( needle ) ) {
					matchingDirs.add( mdd.getSession().toString() );
					//					System.out.println( "Matched: " + mdd );
				}
			}
			if( matchingDirs.isEmpty() )
				return Errno.ENOENT;
			for( String s : matchingDirs )
				filler.add( s, s.hashCode(),
							FuseFtypeConstants.TYPE_FILE| 0644 );
			return 0;
		}

		return Errno.ENOENT;
	}

	/*
	  If open returns a filehandle by calling FuseOpenSetter.setFh()
	  method, it will be passed to every method that supports 'fh'
	  argument
	*/
	@Override
	public int open( String path, int flags, FuseOpenSetter openSetter )
		throws FuseException {

		Collection<ManagedDiskDescriptor> mdds = descriptors();
		String details = path.substring( 1 );
		Matcher m = MANAGEDDISKDESCRIPTORPATHREGEX.matcher( details );
		if( m.matches() ) {
			String diskID = m.group(1);
			String sessionID = m.group(2);
			ManagedDiskDescriptor matching = null;
			for( ManagedDiskDescriptor mdd : mdds ) {
				if( mdd.getDiskID().equals( diskID ) &&
					mdd.getSession().toString().equals( sessionID ) ) {
					/*
					  System.out.println( "open.Matched: " + mdd +
										" to " + MANAGEDDISKDESCRIPTORPATHREGEX );
					*/
					matching = mdd;
					break;
				}
			}
			if( matching == null )
				return Errno.ENOENT;

			ManagedDisk md = store.locate( matching );
			// LOOK: could be null ??
			//			System.out.println( "open.Matched: " + md );

			try {
				SeekableInputStream sis = md.getSeekableInputStream();
				openSetter.setFh( sis );
				//	System.out.println( "open.rar: " + rar );
				return 0;
			} catch( IOException e ) {
				throw new FuseException( e );
			}
		}
		return Errno.ENOENT;
	}

	
	/**
	  @param path the file to read.  Represents a single managed disk
	  (what/when).

	  @param fh filehandle passed from {@link #open(String, int,
	  fuse.FuseOpenSetter) open)}. We know it represents a
	  SeekableInputStream.

	  @param buf a buffer to store the read data.  Has a known
	  available space, which governs how much data we should/can read.

	  @param offset file offset at which to read.  We seek to it.
	*/
	@Override
	public int read(String path, Object fh, ByteBuffer buf, long offset)
		throws FuseException {

		if( log.isDebugEnabled() )
			log.debug( "read.: " + path );

		SeekableInputStream sis = (SeekableInputStream)fh;
		try {
			sis.seek( offset );
			
			/*
			  We keep building a bigger read buffer for each open 'file'.
			  Remember that any read may be for a smaller byte count
			  than the previous one, so use the 3-arg version of read
			*/
			byte[] ba;
			int nin;
			if( true ) {
				ba = readBuffers.get( fh );
				if( ba == null || buf.remaining() > ba.length ) {
					ba = new byte[buf.remaining()];
					readBuffers.put( fh, ba );
					if( log.isInfoEnabled() )
						log.info( "New read buffer for " + path +
								  " = " + ba.length );
				}
				nin = sis.read( ba, 0, buf.remaining() );
			} else {
				ba = new byte[buf.remaining()];
				nin = sis.read( ba );
			}
			
			if( log.isDebugEnabled() ) {
				log.debug( "sis.read " + nin );
			}
			
			if( nin > -1 )
				buf.put( ba, 0, nin );
			else
				// need this??  does this tells fuse we are at eof???
				buf.put( ba, 0, 0 );
			/*
			  the fuse4j api says we return 0, NOT the byte count written
			  to the ByteBuffer
			*/
			return 0;
		} catch( Exception e ) {
			log.warn( e, e );
			log.warn( path + " " + offset + " " + buf.remaining() );
			throw new FuseException( e );
		}
	}
	
	// fh is filehandle passed from open,
   // isWritepage indicates that write was caused by a writepage
	@Override
	public int write(String path, Object fh, boolean isWritepage,
					 ByteBuffer buf, long offset) throws FuseException {
		return Errno.EROFS;
	}

   // called when last filehandle is closed, fh is filehandle passed from open
	@Override
	public int release(String path, Object fh, int flags) throws FuseException {
		log.info( "Release read buffer for " + path );
		
		readBuffers.remove( fh );
		return 0;
	}

	private final Store store;
	private final int startTime;
	private File mountPoint;
	private final Map<Object,byte[]> readBuffers;
	private final Log log;

	/*
	  We check provided paths as identifying valid managed disk content
	  via these two regexs
	*/
	static final Pattern DISKIDPATHREGEX = Pattern.compile
		( "^(" + ManagedDiskDescriptor.DISKIDREGEX.pattern() + ")/?$" );

	static final Pattern MANAGEDDISKDESCRIPTORPATHREGEX = Pattern.compile
		( "^(" + ManagedDiskDescriptor.DISKIDREGEX.pattern() + ")/(" +
		  Session.SHORTREGEX.pattern() + ")/?$" );
}

// eof
