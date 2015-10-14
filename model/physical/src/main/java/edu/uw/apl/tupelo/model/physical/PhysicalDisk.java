package edu.uw.apl.tupelo.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import edu.uw.apl.commons.devicefiles.DeviceFile;

/**
 * @author Stuart Maclean
 *
 * Use DeviceFile to extract low-level data about what we call a 'PhysicalDisk'.
 *
 */

public class PhysicalDisk implements UnmanagedDisk {

	public PhysicalDisk( File f ) throws IOException {
		file = f;
		deviceFile = new DeviceFile( f );
	}

	@Override
	public long size() {
		return deviceFile.size();
	}

	@Override
	public String getID() {
		return deviceFile.getID();
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream( file );
	}

	@Override
	public File getSource() {
		return file;
	}
	
	final File file;
	final DeviceFile deviceFile;
}

// eof
