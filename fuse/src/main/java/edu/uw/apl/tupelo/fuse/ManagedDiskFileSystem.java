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
import edu.uw.apl.tupelo.model.RandomAccessRead;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

/**
 * Note how we access the 'Store' object totally by the base Store
 * interface.  We do NOT need to know here HOW the Store is
 * implemented (though of course the likely implementation is a
 * FilesystemStore.
 */
public class ManagedDiskFileSystem implements Filesystem3 {

	public ManagedDiskFileSystem( Store s ) {
		store = s;
		startTime = (int) (System.currentTimeMillis() / 1000L);
		readBuffers = new HashMap<Object,byte[]>();
		log = LogFactory.getLog( getClass() );
	}

	public void mount( File mountPoint, boolean ownThread ) throws Exception {
		if( !mountPoint.isDirectory() )
			throw new IllegalArgumentException( "Mountpoint not a dir: " +
												mountPoint );
		this.mountPoint = mountPoint;
		/*
		  The -f says no fork, we need this!!
		  The -s says single-threaded, we need this!!
		*/
		String[] args = { mountPoint.getPath(), "-f", "-s"  };
		if( ownThread ) {
			ThreadGroup tg = new ThreadGroup( "MDFS.Threads" );
			FuseMount.mount( args, this, tg, log );
		} else {
			FuseMount.mount( args, this, log );
		}
	}

	public void umount() throws Exception {
		Process p = Runtime.getRuntime().exec( "fusermount -u " + mountPoint );
		p.waitFor();
	}

	// LOOK: how do we handle io errors from store?
	private Collection<ManagedDiskDescriptor> descriptors() {
		try {
			return store.enumerate();
		} catch( IOException ioe ) {
			return Collections.EMPTY_LIST;
		}
	}
	
	@Override
	public int getattr( String path, FuseGetattrSetter getattrSetter )
		throws FuseException {
		if( log.isTraceEnabled() ) {
			log.trace( "getattr " + path );
		}

		//		System.out.println( "getattr: " + path );

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
			int size = (int)md.size();
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
				System.out.println( mdd );
				if( mdd.getDiskID().equals( needle ) ) {
					matchingDirs.add( mdd.getSession().toString() );
					System.out.println( "Matched: " + mdd );
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
				RandomAccessRead rar = md.getRandomAccessRead();
				openSetter.setFh( rar );
				//	System.out.println( "open.rar: " + rar );
				return 0;
			} catch( IOException e ) {
				throw new FuseException( e );
			}
		}
		return Errno.ENOENT;
	}

	
	// fh is filehandle passed from open
	public int read(String path, Object fh, ByteBuffer buf, long offset)
		throws FuseException {

		//		System.out.println( "read.: " + path );

		RandomAccessRead rar = (RandomAccessRead)fh;
		try {
			rar.seek( offset );
			
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
				nin = rar.read( ba, 0, buf.remaining() );
			} else {
				ba = new byte[buf.remaining()];
				nin = rar.read( ba );
			}
			
			if( log.isDebugEnabled() ) {
				log.debug( "rar.read " + nin );
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
	public int write(String path, Object fh, boolean isWritepage,
					 ByteBuffer buf, long offset) throws FuseException {
		return Errno.EROFS;
	}

   // called on every filehandle close, fh is filehandle passed from open
	public int flush(String path, Object fh) throws FuseException {
		//log.info( "flush" );
		return 0;
	}

   // called when last filehandle is closed, fh is filehandle passed from open
	public int release(String path, Object fh, int flags) throws FuseException {
		log.debug( "release" );
		readBuffers.remove( fh );
		return 0;
	}

   // Synchronize file contents, fh is filehandle passed from open,
   // isDatasync indicates that only the user data should be flushed, not the meta data
	public int fsync(String path, Object fh, boolean isDatasync)
		throws FuseException {
		return 0;
	}


	public int readlink(String path, CharBuffer link) throws FuseException {
		//log.info( "readlink" );
		return 0;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		//		log.info( "mkdir" );
		return 0;
	}

	public int unlink(String path) throws FuseException {
		return 0;
	}

	public int rmdir(String path) throws FuseException {
		return 0;
	}

	public int symlink(String from, String to) throws FuseException {
		return 0;
	}

	public int rename(String from, String to) throws FuseException {
		return 0;
	}

	public int link(String from, String to) throws FuseException {
		return 0;
	}

	public int chmod(String path, int mode) throws FuseException {
		return 0;
	}
	
	public int chown(String path, int uid, int gid) throws FuseException {
		return 0;
	}
	
	public int truncate(String path, long size) throws FuseException {
		return 0;
	}
	

	public int utime(String path, int atime, int mtime) throws FuseException {
		//		log.info( "utime" );
		return 0;
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		//log.info( "statfs" );
		return 0;
	}

	public int getxattr(String path, String name, ByteBuffer dst, int position)
		throws FuseException, BufferOverflowException {

		log.debug( "getxattr " + path );

        if( path.equals( "/" ) )
			return Errno.ENOATTR;
		/*
		  try {
			VolumeContainer vc = locateContainer( path );
			if( vc == null )
				return Errno.ENOENT;
		} catch( IOException ioe ) {
			throw new FuseException( ioe );
		}
		*/
		return Errno.ENOATTR;
    }
	
    public int setxattr(String path, String name, ByteBuffer value,
						int flags, int position) throws FuseException {
        return Errno.EROFS;
    }

    public int removexattr(String path, String name) throws FuseException {
        return Errno.EROFS;
    }

    public int listxattr(String path, XattrLister lister) throws FuseException {
		log.debug( "listxattr " + path );
        if( path.equals( "/" ) )
			return Errno.ENOATTR;
		/*
		  try {
			VolumeContainer vc = locateContainer( path );
			if( vc == null )
				return Errno.ENOENT;
		} catch( IOException ioe ) {
			throw new FuseException( ioe );
		}
		*/
		return Errno.ENOATTR;
    }
	
    public int getxattrsize(String path, String name,
							FuseSizeSetter sizeSetter) throws FuseException {
        if( !(path.equals( "/" ) || path.equals( "/foo" ) ) )
            return Errno.ENOENT;

		return Errno.ENOATTR;
    }


	private final Store store;
	private final int startTime;
	private File mountPoint;
	private final Map<Object,byte[]> readBuffers;
	private final Log log;
	
	static final Pattern DISKIDPATHREGEX = Pattern.compile
		( "^(" + ManagedDiskDescriptor.DISKIDREGEX.pattern() + ")/?$" );

	static final Pattern MANAGEDDISKDESCRIPTORPATHREGEX = Pattern.compile
		( "^(" + ManagedDiskDescriptor.DISKIDREGEX.pattern() + ")/(" +
		  Session.SHORTREGEX.pattern() + ")/?$" );

}

// eof
