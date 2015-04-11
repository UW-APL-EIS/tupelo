package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;

/**
 * Given a ManagedDisk, as would be stored on disk in a Tupelo store,
 * we read all its data, in grains, and md5 each grain.  We see how
 * long all the md5 'grain hashing' takes.  It seems to be
 * sloooooooow.
 */

public class ManagedDiskDigestTimingTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void test32m() throws IOException {
		File f = new File( "src/test/resources/32m.zero" );
		if( !f.exists() )
			return;
	}

	// Nuga2 is a 10GB disk from a Win XP vm...
	public void testNuga2() throws IOException {
		File f = new File( "data/nuga2.dd" );
		if( !f.exists() )
			return;
		File managed = toManaged( f );
		testSingleDigest( managed );
		testManyDigest( managed );
	}

	/*
	  Desktop-keyed is a 40GB disk from an Ubuntu Linux vm constructed
	  as part of a packer/vagrant ISO-to-usable-VM tool chain.

	  Of course the original unmanaged disk would be a VirtualDisk
	  (specifically, a .vmdk), but we have externally converted this
	  to a DiskImage (a .dd file) since that is what is used here.
	*/
	public void testDesktopKeyed() throws IOException {
		File f = new File( "data/desktop-keyed.dd" );
		if( !f.exists() )
			return;
		File managed = toManaged( f );
		testSingleDigest( managed );
		testManyDigest( managed );
	}

	private File toManaged( File unmanaged ) throws IOException {
		File out = new File( unmanaged.getName() + ManagedDisk.FILESUFFIX );
		if( out.exists() )
			return out;
		System.out.println( "Creating " + out + " from " + unmanaged );
		UnmanagedDisk ud = new DiskImage( unmanaged );
		ManagedDisk md = new StreamOptimizedDisk( ud, Session.CANNED );
		FileOutputStream fos = new FileOutputStream( out );
		md.writeTo( fos );
		fos.close();
		return out;
	}
		
	private void testSingleDigest( File managed ) throws IOException {
		System.out.println( "Single Digest: " + managed );
		long start = System.currentTimeMillis();
		
		ManagedDisk md = ManagedDisk.readFrom( managed );
		System.out.println( "Managed Size: " + md.size() );
		
		MessageDigest mdg = null;
		try {
			mdg = MessageDigest.getInstance( ManagedDisk.DIGESTALGORITHM );
		} catch( NoSuchAlgorithmException never ) {
		}
		System.out.println( "Digest Algorithm: " + mdg.getAlgorithm() );

		int grainCount = (int)(Utils.alignUp( md.size(), md.grainSizeBytes() ) /
							   md.grainSizeBytes());
		byte[] grain = new byte[(int)md.grainSizeBytes()];
		InputStream is = md.getInputStream();
		ManagedDiskDigest digest = new ManagedDiskDigest();
		for( int g = 1; g <= grainCount; g++ ) {
			//	int nin = dis.read( grain );
			int nin = is.read( grain );
			/*
			  Only the last read could/should return a partial grain,
			  and that would be if the data size not a multiple of
			  grainSize, which is OK.
			*/
			if( nin != grain.length && g < grainCount ) {
				throw new IllegalStateException( "Partial read (" +
												 g + "/" +
												 grainCount + "). Fix!" );
			}
			byte[] hash = mdg.digest( grain );
			digest.add( hash );
			mdg.reset();
		}
		//		dis.close();
		System.out.println( "Digest Length: " + digest.size() );
		is.close();

		long stop = System.currentTimeMillis();
		System.out.println( "Elapsed: " + (stop - start) / 1000 );

		
	}

	private void testManyDigest( File managed ) throws IOException {
		System.out.println( "Many Digest: " + managed );
		long start = System.currentTimeMillis();
		
		ManagedDisk md = ManagedDisk.readFrom( managed );
		System.out.println( "Managed Size: " + md.size() );
		
		int grainCount = (int)(Utils.alignUp( md.size(), md.grainSizeBytes() ) /
							   md.grainSizeBytes());
		byte[] grain = new byte[(int)md.grainSizeBytes()];
		InputStream is = md.getInputStream();
		ManagedDiskDigest digest = new ManagedDiskDigest();

		for( int g = 1; g <= grainCount; g++ ) {
			MessageDigest mdg = null;
			try {
				mdg = MessageDigest.getInstance( ManagedDisk.DIGESTALGORITHM );
			} catch( NoSuchAlgorithmException never ) {
			}
			if( g == 1 )
				System.out.println( "Digest Algorithm: " + mdg.getAlgorithm() );
			//	int nin = dis.read( grain );
			int nin = is.read( grain );
			/*
			  Only the last read could/should return a partial grain,
			  and that would be if the data size not a multiple of
			  grainSize, which is OK.
			*/
			if( nin != grain.length && g < grainCount ) {
				throw new IllegalStateException( "Partial read (" +
												 g + "/" +
												 grainCount + "). Fix!" );
			}
			byte[] hash = mdg.digest( grain );
			digest.add( hash );
		}
		//		dis.close();
		System.out.println( "Digest Length: " + digest.size() );
		is.close();

		long stop = System.currentTimeMillis();
		System.out.println( "Elapsed: " + (stop - start) / 1000 );

		
	}

}

// eof
