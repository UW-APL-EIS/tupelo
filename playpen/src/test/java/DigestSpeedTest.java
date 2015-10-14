import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//import fr.cryptohash.MD5;

/**
 */
public class DigestSpeedTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testSPHLIB() {
	}

	/*
	  This is 1GB of all zeros being fed through JDK's own sha1.  On
	  rejewski, this takes on the order 6-7 secs.  In comparison, the
	  'reference' command line equivalent (likely C programs) computes
	  same result in 3.9 secs:

	  $ time dd if=/dev/zero bs=1M count=1024 | sha1sum

	  AND that has the likely bottleneck of a 4K (64K??) pipe.

	  Actually, the pipe does as well as access via disk.  The sha1sum part below runs
	  in about same time (3.8 secs)

	  $ dd if=/dev/zero bs=1M count=1024 > 1GB
	  $ sha1sum 1GB

	  The second of these is more realistic for Tupelo, since we have
	  ManagedDisk data on disk and wish to compute its digest.

	  4 secs per GB is better than 7 secs per GB when its
	  tens/hundreds of GB and not just 1!  Should we bite the bullet
	  and implement a JNI wrapper for digesting??
	  
	*/
	public void test_SHA1_JDK() {
		MessageDigest mdg = null;
		try {
			mdg = MessageDigest.getInstance( "sha1" );
		} catch( NoSuchAlgorithmException never ) {
		}
		byte[] ba = new byte[1024*1024];

		long start = System.currentTimeMillis();
		for( int i = 1; i <= 1024; i++ ) {
			mdg.update( ba );
		}
		byte[] h = mdg.digest();
		long end = System.currentTimeMillis();
		System.out.println( "SHA1 Elapsed: " + (end-start) );
		System.out.printf( "%02x\n", h[0] & 0xff );
	}

	/*
	  See note above for sha1 comparison between Java and reference
	  impl.  For MD5, difference even more pronounced.  Reference 2.7
	  secs, Java 4.8 secs (on rejewski).
	*/
	public void test_MD5_JDK() {
		MessageDigest mdg = null;
		try {
			mdg = MessageDigest.getInstance( "md5" );
		} catch( NoSuchAlgorithmException never ) {
		}
		byte[] ba = new byte[1024*1024];

		long start = System.currentTimeMillis();
		for( int i = 1; i <= 1024; i++ ) {
			mdg.update( ba );
		}
		byte[] h = mdg.digest();
		long end = System.currentTimeMillis();
		System.out.println( "MD5 Elapsed: " + (end-start) );
		System.out.printf( "%02x\n", h[0] & 0xff );
	}
}

// eof
