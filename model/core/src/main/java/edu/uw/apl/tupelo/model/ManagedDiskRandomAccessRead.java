package edu.uw.apl.tupelo.model;

import java.io.InputStream;
import java.io.IOException;

/**
 * An abstract RandomAccessRead capturing the logic shared between various
 * Tupelo managed disk classes which offer the user an RandomAccessRead
 * interface for reading data.  Subclasses must provide:
 *
 * close
 * seek
 * readImpl
 *
 * @see StreamOptimizedDisk.RandomAccessRead
 */

abstract public class ManagedDiskRandomAccessRead implements RandomAccessRead {

	protected ManagedDiskRandomAccessRead( long size ) {
		this.size = size;
		posn = 0;
	}

	abstract public int readImpl( byte[] b, int off, int len )
		throws IOException;
	
	@Override
	public long length() throws IOException {
		return size;
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
	public int read( byte[] ba ) throws IOException {
		return read( ba, 0, ba.length );
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
		
		int n = readImpl( b, off, len );
		if( n == -1 ) {
			throw new IOException();
		}

		return n;
	}
	
	protected final long size;
	protected long posn;
}

// eof
