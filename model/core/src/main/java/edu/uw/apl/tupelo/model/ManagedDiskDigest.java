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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;


public class ManagedDiskDigest {

	public ManagedDiskDigest( int bins ) {
		grainHashes = new ArrayList<byte[]>( bins );
	}

	public ManagedDiskDigest() {
		this( 1024 );
	}

	public void add( byte[] ba ) {
		//byte[] copy = new byte[ba.length];
		//System.arraycopy( ba, 0, copy, 0, ba.length );
		grainHashes.add( ba );
	}

	public byte[] get( int i ) {
		return grainHashes.get(i);
	}

	public int size() {
		return grainHashes.size();
	}
	
	public void writeTo( Writer w ) throws IOException {
		BufferedWriter bw = new BufferedWriter( w, 1 << 20 );
		PrintWriter pw = new PrintWriter( bw );
		for( byte[] gh : grainHashes ) {
			String hashHex = new String( Hex.encodeHex( gh ) );
			pw.println( hashHex );
		}
		pw.close();
	}

	static public ManagedDiskDigest readFrom( Reader r ) throws IOException {
		ManagedDiskDigest result = new ManagedDiskDigest( 1024 );
		BufferedReader br = new BufferedReader( r );
		String line = null;
		while( (line = br.readLine()) != null ) {
			try {
				byte[] grainHash = Hex.decodeHex( line.toCharArray() );
				result.add( grainHash );
			} catch( DecoderException de ) {
				throw new IOException( de );
			}
		}
		br.close();
		return result;
	}

	private final List<byte[]> grainHashes;
}

// eof
