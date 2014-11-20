package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CompressionAlgorithmsTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testWriteNuga2() throws IOException {
		File f = new File( "data/nuga2.dd" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );

		testWrite( md, ManagedDisk.Compressions.DEFLATE );
		testWrite( md, ManagedDisk.Compressions.GZIP );
		testWrite( md, ManagedDisk.Compressions.SNAPPY );
	}

	private void testWrite( ManagedDisk md, ManagedDisk.Compressions c )
		throws IOException {

		long start = System.currentTimeMillis();
		md.setCompression( c );
		ManagedDiskDescriptor mdd = md.getDescriptor();
		File out = new File( mdd.getDiskID() + ManagedDisk.FILESUFFIX +
							 "-compress." + c.ordinal() );
		
		FileOutputStream fos = new FileOutputStream( out );
		BufferedOutputStream bos = new BufferedOutputStream( fos, 1024*64 );
		md.writeTo( bos );
		bos.close();
		fos.close();
		long stop = System.currentTimeMillis();

		System.out.println( out + " -> " + (stop-start)/1000 );
	}

	public void testReadNuga2() throws IOException {

		ManagedDisk.Compressions[] cs = {
			ManagedDisk.Compressions.DEFLATE,
			ManagedDisk.Compressions.GZIP,
			ManagedDisk.Compressions.SNAPPY,
		};
		for( ManagedDisk.Compressions c : cs ) {
			File f = new File( "nuga2.dd" + ManagedDisk.FILESUFFIX +
							   "-compress." + c.ordinal() );
			System.out.println( f );
			if( !f.exists() )
				return;
			ManagedDisk md = ManagedDisk.readFrom( f );
			InputStream is = md.getInputStream();
			long start = System.currentTimeMillis();
			String md5 = Utils.md5sum( is );
			is.close();
			long stop = System.currentTimeMillis();
			System.out.println( md5 );
			
			System.out.println( f + " -> " + (stop-start)/1000 );
		}
	}
}

// eof
