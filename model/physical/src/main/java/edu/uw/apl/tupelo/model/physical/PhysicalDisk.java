package edu.uw.apl.tupelo.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import edu.uw.apl.nativelibloader.NativeLoader;

/*
  TODO: the JNI code needed for deriving device size in bytes and disk
  serial id (aka vendor ID).  We have this, just cut/paste here
*/

public class PhysicalDisk implements UnmanagedDisk {

	public PhysicalDisk( File f ) throws IOException {
		if( f == null )
			throw new IllegalArgumentException( "Null file!" );

		/*
		  Fail early with IOException if no such file, before jni...
		*/
		FileInputStream fis = new FileInputStream( f );
		fis.close();

		// Java reports 0 for the size of a device file...
		long len = f.length();
		if( len != 0 )
			throw new IllegalArgumentException( f + ": Unexpected length "
												+ len );
		disk = f;
	}

	public long size() {
		return size( disk.getPath() );
	}

	public String getID() {
		String v = vendorID( disk.getPath() );
		if( v != null )
			v = v.trim();
		String p = productID( disk.getPath() );
		if( p != null )
			p = p.trim();
		String s = serialNumber( disk.getPath() );
		if( s != null )
			s = s.trim();
		return v + "-" + p + "-" + s;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		if( true) {
			return new FileInputStream( disk );
		} else {
			FileInputStream fis = new FileInputStream( disk );
			int sz = 1024 * 1024 * 32;
			BufferedInputStream bis = new BufferedInputStream( fis, sz );
			return bis;
		}
			   
	}

	private native long size( String path );

	private native String vendorID( String path );
	private native String productID( String path );
	private native String serialNumber( String path );
	
	final File disk;

	static private final String artifact = "tupelo-model-physical";
    static {
		try {
			NativeLoader.load( PhysicalDisk.class, artifact );
		} catch( Throwable t ) {
			throw new ExceptionInInitializerError( t );
		}
    }
}

// eof
