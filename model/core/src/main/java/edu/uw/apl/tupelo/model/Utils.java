/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

