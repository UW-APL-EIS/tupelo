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
				}
				System.arraycopy( buffer, 0, b, off, actual );
				return actual;
			}

			private byte[] buffer;
		}
	}
