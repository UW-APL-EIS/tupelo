package edu.uw.apl.tupelo.model;

import java.io.IOException;

/**
   Partial implementation of the RandomAccessRead interface.
*/

abstract public class AbstractRandomAccessRead
	implements RandomAccessRead {

	@Override
	public int read() throws IOException {
		byte[] ba = new byte[1];
		int n = read( ba, 0, 1 );
		if( n == -1 )
			return -1;
		return ba[0] & 0xff;
	}

	@Override
	public int read( byte[] b ) throws IOException {
		return read( b, 0, b.length );
	}
}

// eof