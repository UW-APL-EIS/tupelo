package edu.uw.apl.tupelo.model;

import java.io.IOException;

public interface RandomAccessRead {
	
	public void close() throws IOException;
	public long length() throws IOException;
	public void seek( long pos ) throws IOException;

	public int read() throws IOException;
	public int read( byte[] b ) throws IOException;
	public int read( byte[] b, int off, int len ) throws IOException;
}

// eof
