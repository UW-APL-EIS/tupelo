package edu.uw.apl.tupelo.model;

import java.io.InputStream;
import java.io.IOException;
import java.util.Random;

public class RandomDisk extends MemoryDisk {

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
		
		@Override
		public int readImpl( byte[] b, int off, int len )
			throws IOException {
			
			// Do min in long space, since size - posn may overflow int...
			long actualL = Math.min( size - posn, len );
			
			// Cannot blindly coerce a long to int, result could be -ve
			int actual = actualL > Integer.MAX_VALUE ? Integer.MAX_VALUE :
				(int)actualL;
			
			if( actual > buffer.length ) {
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
