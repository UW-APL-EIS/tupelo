package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;

public class Utils {
	
	static public String md5sum( File f ) throws IOException {
		return md5sum( f, 1024*1024 );
	}

	static public String md5sum( File f, int blockSize ) throws IOException {
		try( FileInputStream fis = new FileInputStream( f ) ) {
				return md5sum( fis, blockSize );
			}
	}

	static public String md5sum( InputStream is ) throws IOException {
		return md5sum( is, 1024*1024 );
	}
	
	static public String md5sum( InputStream is, int blockSize )
		throws IOException {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance( "md5" );
		} catch( Exception e ) {
			// never
		}
		try( DigestInputStream dis = new DigestInputStream( is, md5 ) ) {
				byte[] ba = new byte[blockSize];
				while( true ) {
					int nin = dis.read( ba );
					if( nin < 0 )
						break;
				}
				byte[] hash = md5.digest();
				return Hex.encodeHexString( hash );
			}
	}

	/**
	 * Example: alignUp( 700, 512 ) -> 1024
	 */
	static public long alignUp( long b, long a ) {
		return (long)(Math.ceil( (double)b / a ) * a);
	}

}

// eof

