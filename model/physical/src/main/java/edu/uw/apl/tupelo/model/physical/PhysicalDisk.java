package edu.uw.apl.tupelo.model;

import java.io.File;

/*
  TODO: the JNI code needed for deriving device size in bytes and disk
  serial id (aka vendor ID).  We have this, just cut/paste here
*/

public class PhysicalDisk {

	public PhysicalDisk( File f ) {
		if( f == null )
			throw new IllegalArgumentException( "Null file!" );
		disk = f;
	}

	public long size() {
		// LOOK: complete
		return 1024L * 1024L;
	}

	public String getID() {
		// LOOK: complete
		return "PhysicalDisk.TODO!!";
	}
	
	final File disk;
}

// eof
