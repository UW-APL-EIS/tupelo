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
 * Use DeviceFile to extract low-level data about what we call in
 * Tupelo a 'PhysicalDisk'.
 *
 * The DeviceFile class is a split Java/C beast. Currently, the C
 * parts of DeviceFile only built for Linux (MacOS, Windows to do).
 * That means that any use of this PhysicalDisk on
 * those platforms which produce an UnsatisfiedLinkError.  In these
 * cases, we <em>do</em> stagger on and the PhysicalDisk instance is
 * built.  However, actual device id and size will be unknown and we
 * use local defaults.
 *
 * @see {@link https://github.com/uw-dims/device-files}
 *
 */

public class PhysicalDisk implements UnmanagedDisk {

	public PhysicalDisk( File f ) throws IOException {
		file = f;

		/*
		  If DeviceFile fails to load its C parts, either due to being
		  run on a platform for which there are no C parts, or if that
		  loading disabled explicitly, its id and size calls will
		  fail.  We notice that here, and fill in defaults.
		*/
		try {
			DeviceFile df = new DeviceFile( f );
			id = df.getID();
			size = df.size();
		} catch( UnsatisfiedLinkError ull ) {
			System.err.println( f + ": Device details unavailable." );
			id = f.getPath();
			size = 0;
		}
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream( file );
	}

	@Override
	public File getSource() {
		return file;
	}
	
	private final File file;
	private String id;
	private long size;
}

// eof
