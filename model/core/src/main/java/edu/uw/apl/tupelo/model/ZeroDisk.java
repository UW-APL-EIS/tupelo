package edu.uw.apl.tupelo.model;

import java.io.InputStream;
import java.io.IOException;

/**
 * @author Stuart Maclean
 *
 * A fake 'disk' in which all contents are zeros.  Any read just
 * returns zeroes. A variation of {@link MemoryDisk}, since has no
 * disk-backed data at all.
 *
 * @see RandomDisk
 * @see MemoryDisk
 */

public class ZeroDisk extends MemoryDisk {

	/**
	 * @param readSpeedBytesPerSecond - how many bytes can be
	 * read per second from this fake disk.  Used to put realistic
	 * load on read operations. 200 MBs-1 is reasonable.
	 */
	public ZeroDisk( long sizeBytes, long readSpeedBytesPerSecond ) {
		this( sizeBytes, readSpeedBytesPerSecond,
			  ZeroDisk.class.getSimpleName() + "-" + sizeBytes );
	}
	
	/**
	 * @param speedBytesPerSecond - how many bytes can be read per second
	 * from this fake disk.  Used to put realistic load on read
	 * operations.
	 */
	public ZeroDisk( long sizeBytes, long speedBytesPerSecond, String id ) {
		super( sizeBytes, speedBytesPerSecond, id );
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
