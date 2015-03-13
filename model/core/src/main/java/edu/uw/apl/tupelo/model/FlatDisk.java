package edu.uw.apl.tupelo.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

/**
 * The FlatDisk variant of ManagedDisks simply prepends a
 * ManagedDisk.Header on the front of the input (Unmanaged) data.  The
 * FlatDisk size is therefore essentially equal to the input size.
 
 * The FlatDisk is thus really only for either testing or for
 * (infeasibly) small disk images.  Trying to store an 250GB physical
 * disk (e.g. /dev/sda) as a FlatDisk is asking for trouble.  Use
 * StreamOptimizedDisks instead!
 *
 * @see StreamOptimizedDisks
 */

public class FlatDisk extends ManagedDisk {

	public FlatDisk( UnmanagedDisk ud, Session session ) {
		super( ud, null );

		/*
		  A FlatDisk holds ALL its own data, so needs no parent.  This
		  is true even if the managed data has ancestors. Since the
		  managed data is simply appended to the Header as is, we have
		  need for any 'grain' logic at all.  The only constraint
		  is that it be a whole number of sectors.
		*/
		long len = unmanagedData.size();
		checkSize( len );

		String diskID = unmanagedData.getID();
		UUID parent = Constants.NULLUUID;
		long capacity = len / Constants.SECTORLENGTH;
		// Currently we have just a single sector header...
		long overhead = 1;
		header = new Header( diskID, session, DiskTypes.FLAT, parent,
							 capacity, GRAINSIZE_DEFAULT, overhead );
		header.dataOffset = Header.SIZEOF;
	}

	// Called from ManagedDisk.readFrom()
	public FlatDisk( File managedData, Header h ) {
		super( null, managedData );
		header = h;
	}

	@Override
	public void setParentDigest( ManagedDiskDigest grainHashes ) {
	}

	@Override
	public void reportMetaData() throws IOException {
	}

	@Override
	public void setParent( ManagedDisk md ) {
		throw new IllegalStateException( getClass() + ".setParent!!" );
	}

	@Override
	public void writeTo( OutputStream os ) throws IOException {
		if( unmanagedData == null )
			throw new IllegalStateException
				( header.diskID + ": unmanagedData null" );
		InputStream is = unmanagedData.getInputStream();

		header.writeTo( os );
		// IOUtils uses 4k buffers, pah!
		if( false ) {
			byte[] ba = new byte[1024*1024*16];
			IOUtils.copyLarge( is, os, ba );
		} else {
			IOUtils.copyLarge( is, os );
		}
		is.close();
	}

	/**
	 * @param is An InputStream implementation likely to be
	 * participating in a byte count operation for ProgressMonitor
	 * purposes.  All data is to be read from this stream, NOT from
	 * the result of the FlatDisk's own ManagedDisk.getInputStream()
	 */
	@Override
	public void readFromWriteTo( InputStream is, OutputStream os )
		throws IOException {
		header.writeTo( os );
		if( true ) {
			IOUtils.copyLarge( is, os );
		} else {
			byte[] ba = new byte[1024*1024];
			while( true ) {
				int nin = is.read( ba );
				if( nin < 0 )
					break;
				os.write( ba, 0, nin );
			}
		}
	}

	/**
	 * The best a FlatDisk can do to verify that a file on disk really
	 * is the managed representation of the associated unmanaged data
	 * is to compare sizes.  A FlatDisk should simpy be a
	 * Header.SIZEOF bigger than its unmanaged version.
	 *
	 * Note how this impl does NOT read any file content, we could
	 * compare unmanaged + managed content, but how much ? Random
	 * sectors?
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public void verify() throws IOException {
		if( unmanagedData == null ) {
			if( true ) {
				/*
				  Over http, we would have no unmanaged data access,
				  so cannot attempt a verify at all.  The result is then what?
				*/
				return;
			} else {
				throw new IllegalStateException
					( "Verify failed. noUnmanagedData" );
			}
		}
		if( managedData == null )
			throw new IllegalStateException( "Verify failed. noManagedData" );
		long usize = unmanagedData.size();
		long msize = managedData.length();
		if( usize + Header.SIZEOF != msize )
			throw new IllegalStateException( "Verify failed: bad size compare");
	}

	public void writeTo( File f ) throws IOException {
		FileOutputStream fos = new FileOutputStream( f );
		BufferedOutputStream bos = new BufferedOutputStream( fos, 1024*1024 );
		writeTo( bos );
		bos.close();
		fos.close();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if( managedData == null )
			throw new IllegalStateException( "managedData missing" );
		FileInputStream fis = new FileInputStream( managedData );
		fis.skip( header.dataOffset );
		return fis;
	}

	@Override
	public SeekableInputStream getSeekableInputStream() throws IOException {
		return new FlatDiskRandomAccessRead();
	}

	class FlatDiskRandomAccessRead extends SeekableInputStream {
		FlatDiskRandomAccessRead() throws IOException {
			super( size() );
			raf = new RandomAccessFile( managedData, "r" );
			raf.seek( header.dataOffset );
		}

		@Override
		public void close() throws IOException {
			raf.close();
		}

		@Override
		public void seek( long s ) throws IOException {
			// according to java.io.RandomAccessFile, no restriction on seek
			raf.seek( header.dataOffset + s );
			posn = s;
		}

		/**
		   For the array read, we shall attempt to satisy the length
		   requested, even if it is takes us many reads (of the
		   physical file) to do so.  While the contract for
		   InputStream is that any read CAN return < len bytes, for
		   InputStreams backed by file data, users probably expect len
		   bytes back (fewer of course if eof).

		   Further, when using this class with our
		   'ManagedDiskFileSystem', which uses fuse, fuse states that
		   the callback read operation is REQUIRED to return len bytes
		   if they are available (i.e. not read past eof)
		*/
		   
		@Override
		public int readImpl( byte[] ba, int off, int len ) throws IOException {

			long actualL = Math.min( size - posn, len );
			int actual = actualL > Integer.MAX_VALUE ? Integer.MAX_VALUE :
				(int)actualL;

			int total = 0;
			while( total < actual ) {
				int nin = raf.read( ba, off+total, len-total );
				total += nin;
				posn += nin;
			}
			return total;
		}

		private final RandomAccessFile raf;
	}
}

// eof
