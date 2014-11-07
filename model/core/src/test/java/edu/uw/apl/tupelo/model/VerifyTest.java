package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VerifyTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void test1gStreamOptimized() throws IOException {
		File f = new File( "./1g" );
		if( !f.exists() )
			return;
		System.out.println( "test1gStreamOptimized: " + f );
		UnmanagedDisk ud = new DiskImage( f );
		StreamOptimizedDisk sod = new StreamOptimizedDisk( ud, Session.CANNED );
		File mf = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( mf );
		sod.writeTo( fos );
		fos.close();
		sod.setManagedData( mf );
		try {
			sod.verify();
		} catch( IllegalStateException ise ) {
			fail();
		}
	}

	public void test1gFlat() throws IOException {
		File f = new File( "./1g" );
		if( !f.exists() )
			return;
		System.out.println( "test1gFlat: " + f );
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		File mf = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( mf );
		fd.writeTo( fos );
		fos.close();
		fd.setManagedData( mf );
		try {
			fd.verify();
		} catch( IllegalStateException ise ) {
			fail();
		}
	}
}

// eof
