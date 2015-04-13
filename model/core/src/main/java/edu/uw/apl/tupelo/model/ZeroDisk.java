package edu.uw.apl.tupelo.model;

import java.io.InputStream;
import java.io.IOException;

public class ZeroDisk extends MemoryDisk {

	public ZeroDisk( long size ) {
		super( size );
	}
	
	public ZeroDisk( long size, String id ) {
		super( size, id );
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new ZeroInputStream();
	}
	
	class ZeroInputStream extends MemoryDiskInputStream {
		ZeroInputStream() {
			buffer = new byte[0];
		}
		
		/**
		 * @param actual - NOT the len that the caller passed, but
		 * a computed maximum byte count from
		 * {@link MemoryDisk#read(byte[],int,int)}
		 */
		@Override
		protected int readImpl( byte[] b, int off, int actual )
			throws IOException {
				
			if( actual > buffer.length ) {
				// LOOK: could blow up, what if actual=MaxInt ??
				buffer = new byte[actual];
			}
			System.arraycopy( buffer, 0, b, off, actual );
			return actual;
		}

		private byte[] buffer;
	}
}

// eof
