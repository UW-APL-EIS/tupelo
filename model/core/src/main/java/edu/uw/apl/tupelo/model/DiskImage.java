package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 */
 
public class DiskImage implements UnmanagedDisk {

	public DiskImage( File f ) {
		this( f, f.getName() );
	}

	public DiskImage( File f, String givenID ) {
		this.data = f;
		this.id = givenID;
	}

	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public long size() {
		return data.length();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		FileInputStream fis = new FileInputStream( data );
		return fis;
	}

	@Override
	public File getSource() {
		return data;
	}

	private final File data;
	private final String id;
	
}

// eof
