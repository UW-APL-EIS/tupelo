package edu.uw.apl.tupelo.model;

import java.io.InputStream;
import java.io.IOException;
import java.util.Random;

/**
 * A test/fake implementation of an UnmanagedDisk.  This one is an
 * in-memory disk (needs no real backing disk) which produces random
 * data when read.  The 'random' nature does NOT give a new random
 * buffer for each read, it just re-uses the same buffer.  The only
 * way the local buffer is filled with fresh 'random' data is when a
 * larger buffer is needed, due to a larger length being read than we
 * had previously seen.
 */
public class RandomDisk extends MemoryDisk {

	/**
	 * @param size byte count for this disk
	 */
	public RandomDisk( long size ) {
		super( size );
	}
	
	public RandomDisk( long size, String id ) {
		super( size, id );
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new RandomDiskInputStream();
	}

	class RandomDiskInputStream extends MemoryDiskInputStream {
		RandomDiskInputStream() {
			buffer = new byte[0];
			rng = new Random();
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
				for( int i = 0; i < buffer.length; i++ )
					buffer[i] = (byte)rng.nextInt();
			}
			System.arraycopy( buffer, 0, b, off, actual );
			return actual;
		}
		
		private byte[] buffer;
		private final Random rng;
	}
}

// eof
