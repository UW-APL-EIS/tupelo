package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

public class FlatDisk extends ManagedDisk {

	public FlatDisk( File rawImage, long length, String diskID, Session session,
					 UUID parent ) {
		this.rawImage = rawImage;
		this.managedDisk = null;
		header = new Header( diskID, session, DiskTypes.FLAT, parent,
							 length / Constants.SECTORLENGTH, 128 );
		header.dataOffset = Header.SIZEOF;
	}

	public FlatDisk( File managedDisk, Header h ) {
		this.managedDisk = managedDisk;
		header = h;
		this.rawImage = null;
	}

	@Override
	public boolean hasParent() {
		return false;
	}

	@Override
	public void setParent( ManagedDisk md ) {
		throw new IllegalStateException( getClass() + ".setParent!!" );
	}

	@Override
	public void writeTo( File f ) throws IOException {
		FileOutputStream fos = new FileOutputStream( f );
		header.writeTo( fos );
		FileUtils.copyFile( rawImage, fos );
		fos.close();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		FileInputStream fis = new FileInputStream( managedDisk );
		fis.skip( header.dataOffset );
		return fis;
	}

	@Override
	public RandomAccessRead getRandomAccessRead() throws IOException {
		return new FlatDiskRandomAccessRead();
	}

	class FlatDiskRandomAccessRead extends AbstractRandomAccessRead {
		FlatDiskRandomAccessRead() throws IOException {
			raf = new RandomAccessFile( managedDisk, "r" );
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

	// Only one or the other is valid, never both. 
	private final File rawImage;		// for creating/writing a ManagedDisk
	private final File managedDisk;		// for loading a ManagedDisk
	
}

// eof
