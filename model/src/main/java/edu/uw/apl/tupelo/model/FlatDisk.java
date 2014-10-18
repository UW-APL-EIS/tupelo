package edu.uw.apl.tupelo.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

/**
 * The FlatDisk variant of ManagedDisks are really only for either
 * testing or for (infeasibly) small disk images.  Trying to store an
 * 80GB disk as a FlatDisk is asking for trouble.  Use StreamOptimizedDisks
 * instead!
 *
 * @see StreamOptimizedDisks
 */

public class FlatDisk extends ManagedDisk {

	/**
	 * @param rawData - cannot be a whole disk, e.g. /dev/sda, since
	 * length will be (incorrectly for our purposes) read as 0.  Must
	 * instead be some form of disk image file.
	 */
	public FlatDisk( File rawData, String diskID, Session session ) {
		super( rawData, null );

		/*
		  A FlatDisk holds ALL its own data, so needs no parent.  This
		  is true even if the managed data has ancestors. Since the
		  managed data is simply appended to the Header as is, we have
		  need for any 'grain' logic at all.  The only constraint
		  is that it be a whole number of sectors.
		*/
		UUID parent = Constants.NULLUUID;
		long len = rawData.length();
		if( len % Constants.SECTORLENGTH != 0 ) {
			throw new IllegalArgumentException
				( "Data length (" + len +
				  ") must be a multiple of " + Constants.SECTORLENGTH );
		}
		long capacity = len / Constants.SECTORLENGTH;
		header = new Header( diskID, session, DiskTypes.FLAT, parent,
							 capacity, GRAINSIZE_DEFAULT );
		header.dataOffset = Header.SIZEOF;
	}

	// Called from ManagedDisk.readFrom()
	public FlatDisk( File managedData, Header h ) {
		super( null, managedData );
		header = h;
	}

	@Override
	public void setParent( ManagedDisk md ) {
		throw new IllegalStateException( getClass() + ".setParent!!" );
	}

	public void writeTo( OutputStream os ) throws IOException {
		if( rawData == null )
			throw new IllegalStateException( "rawData missing" );
		header.writeTo( os );
		FileUtils.copyFile( rawData, os );
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
	public RandomAccessRead getRandomAccessRead() throws IOException {
		return new FlatDiskRandomAccessRead();
	}

	class FlatDiskRandomAccessRead extends AbstractRandomAccessRead {
		FlatDiskRandomAccessRead() throws IOException {
			raf = new RandomAccessFile( managedData, "r" );
			raf.seek( header.dataOffset );
			size = size();
			posn = 0;
		}

		@Override
		public void close() throws IOException {
			raf.close();
		}

		@Override
		public long length() throws IOException {
			return size;
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
		public int read( byte[] ba, int off, int len ) throws IOException {

			/*
			  checks from the contract for InputStream, which docs for
			  RandomAccessFile say it honors
			*/
			if( ba == null )
				throw new NullPointerException();
			if( off < 0 || len < 0 || off + len > ba.length ) {
				throw new IndexOutOfBoundsException();
			}
			if( len == 0 )
				return 0;
			
			if( posn >= size ) {
				return -1;
			}
			
			long actualL = Math.min( size - posn, len );
			int actual = (int)actualL;
			//logger.debug( "Actual " + actualL + " " + actual );
			int total = 0;
			while( total < actual ) {
				int nin = raf.read( ba, off+total, len-total );
				total += nin;
				posn += nin;
			}
			return total;
		}

		private final RandomAccessFile raf;
		private final long size;
		private long posn;
	}

	
}

// eof
