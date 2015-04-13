package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * An implementation of UnmanagedDisk in which all reads are from
 * local memory.  Has no backing disk at all.  Useful only in testing
 * of course.
 *
 * @see RandomDisk
 * @see ZeroDisk
 */
 
abstract public class MemoryDisk implements UnmanagedDisk {

	protected MemoryDisk( long size ) {
		this( size, "memdisk-" + size );
	}

	protected MemoryDisk( long size, String id ) {
		this.size = size;
		this.id = id;
	}

	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public long size() {
		return size;
	}

	@Override
	public File getSource() {
		return new File( id );
	}
	
	
	abstract class MemoryDiskInputStream extends InputStream {
		MemoryDiskInputStream() {
			posn = 0;
		}
		
		abstract protected int readImpl( byte[] b, int off, int len )
			throws IOException;

		@Override
		public int available() throws IOException {
			// Cannot simply cast 'size - posn' to int, could get -ve value!
			long l = size - posn;
			if( l >= Integer.MAX_VALUE )
				return Integer.MAX_VALUE;
			return (int)l;
		}

		@Override
		public int read() throws IOException {
			byte[] ba = new byte[1];
			int n = read( ba, 0, 1 );
			if( n == -1 )
				return -1;
			return ba[0] & 0xff;
		}
		
		@Override
		public int read( byte[] b, int off, int len ) throws IOException {
			
			// checks from the contract for InputStream...
			if( b == null )
				throw new NullPointerException();
			if( off < 0 || len < 0 || off + len > b.length ) {
				throw new IndexOutOfBoundsException();
			}
			if( len == 0 )
				return 0;

			if( posn >= size )
				return -1;

			
			// Do min in long space, since size - posn may overflow int...
			long actualL = Math.min( size - posn, len );
			
			/*
			  Safe to cast to int, since know len and thus min is at most
			  maxInt, and cannot be -ve since checked above.
			*/
			int actual = (int)actualL;
			int n = readImpl( b, off, actual );
			if( n == -1 ) {
				throw new IOException();
			}
			posn += n;
			return n;
		}

		@Override
		public long skip( long n ) throws IOException {
			if( n < 0 )
				return 0;
			long min = Math.min( n, size-posn );
			posn += min;
			return min;
		}

		private long posn;
	}

	protected final long size;
	protected final String id;
	
}

// eof
