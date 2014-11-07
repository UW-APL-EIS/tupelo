package edu.uw.apl.tupelo.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamOptimizedDiskTest extends junit.framework.TestCase {

	public void testNull() {
	}

	// A sized file which should PASS the 'whole number of grains' test
	public void testSize64k() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		} catch( IllegalArgumentException iae ) {
			fail();
		}
	}
	
	// A sized file which should FAIL the 'whole number of grains' test
	public void testSize1000() throws IOException {
		File f = new File( "src/test/resources/1000" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
			fail();
		} catch( IllegalArgumentException iae ) {
			System.out.println( "Expected: " + iae );
		}
	}

	// A sized file which should FAIL the 'whole number of grains' test
	public void testSize1k() throws IOException {
		File f = new File( "src/test/resources/1k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		try {
			ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
			fail();
		} catch( IllegalArgumentException iae ) {
			System.out.println( "Expected: " + iae );
		}
	}

	public void _testManage32m() throws IOException {
		File f = new File( "src/test/resources/32m.zero" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		File out = new File( "/dev/null" );
		FileOutputStream fos = new FileOutputStream( out );
		md.writeTo( fos );
		fos.close();

	}

	public void testNuga2() throws IOException {
		File f = new File( "data/nuga2.dd" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		File out = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( out );
		md.writeTo( fos );
		fos.close();

	}

	public void testManagedNuga2() throws IOException {
		File f = new File( "nuga2.dd" + ManagedDisk.FILESUFFIX );
		if( !f.exists() )
			return;
		ManagedDisk md = ManagedDisk.readFrom( f );
		InputStream is = md.getInputStream();
		String md5 = Utils.md5sum( is );
		is.close();
		System.out.println( md5 );
		if( false ) {
			byte[] ba = new byte[1024*1024];
			int nin = is.read( ba );
			System.out.println( nin );
		}
	}

	public void test1g() throws IOException {
		File f = new File( "data/sda.1g" );
		if( !f.exists() )
			return;
		System.out.println( "Test " + f );
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		File out = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( out );
		BufferedOutputStream bos = new BufferedOutputStream( fos, 1024*1024 );
		md.writeTo( bos );
		bos.close();
		fos.close();
	}

	public void testManaged1g() throws IOException {
		File f = new File( "sda.1g" + ManagedDisk.FILESUFFIX );
		if( !f.exists() )
			return;
		System.out.println( "Test " + f );
		ManagedDisk md = ManagedDisk.readFrom( f );
		InputStream is = md.getInputStream();
		String md5 = Utils.md5sum( is );
		is.close();
		System.out.println( md5 );
		if( false ) {
			byte[] ba = new byte[1024*1024];
			int nin = is.read( ba );
			System.out.println( nin );
		}
	}

	public void _test32m() throws IOException {
		File f = new File( "src/test/resources/32m.zero" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		File out = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( out );
		md.writeTo( fos );
		fos.close();
		System.out.println( "Writing " + out );
	}

	public void _testManaged32m() throws IOException {
		File f = new File( "32m.zero" );
		File fm = new File( f.getPath() + ManagedDisk.FILESUFFIX );
		if( !fm.exists() )
			return;
		System.out.println( "Reading " + fm );
		ManagedDisk md = ManagedDisk.readFrom( fm );
		InputStream is = md.getInputStream();
		String md5 = Utils.md5sum( is );
		is.close();
		System.out.println( md5 );
		if( false ) {
			byte[] ba = new byte[1024*1024];
			int nin = is.read( ba );
			System.out.println( nin );
		}
	}
	
	public void _testNugaPart() throws IOException {
		File f = new File( "nuga2.128m" );
		//		File f = new File( "nuga2.64m" );
		//		File f = new File( "nuga2.32m" );
		//		File f = new File( "nuga2.1g" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		File out = new File( f.getName() + ManagedDisk.FILESUFFIX );
		FileOutputStream fos = new FileOutputStream( out );
		md.writeTo( fos );
		System.out.println( "Writing " + out );
	}

	public void _testManagedNugaPart() throws IOException {
		File f = new File( "nuga2.128m" );
		//		File f = new File( "nuga2.64m" );
		//		File f = new File( "nuga2.32m" );
		//		File f = new File( "nuga2.1g" );
		File fm = new File( f.getPath() + ManagedDisk.FILESUFFIX );
		if( !fm.exists() )
			return;
		System.out.println( "Reading " + fm );
		ManagedDisk md = ManagedDisk.readFrom( fm );
		InputStream is = md.getInputStream();
		String md5 = Utils.md5sum( is );
		is.close();
		System.out.println( md5 );
		if( false ) {
			byte[] ba = new byte[1024*1024];
			int nin = is.read( ba );
			System.out.println( nin );
		}
	}

}

// eof
