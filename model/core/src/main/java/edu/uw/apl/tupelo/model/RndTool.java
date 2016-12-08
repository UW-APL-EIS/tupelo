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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * @author Stuart Maclean
 *
 */
public class RndTool {

	static public void usage() {
		System.err.println( "Usage: " + RndTool.class +
							" log2size seed" );
		System.exit(1);
	}
	
	static public void main( String[] args ) {
		if( args.length < 2 ) {
			usage();
		}
		int log2size = -1;
		int seed = -1;
		try {
			log2size = Integer.parseInt( args[0] );
			seed = Integer.parseInt( args[1] );
		} catch( RuntimeException re ) {
			usage();
		}
		try {
			Random r = new Random( seed );
			BufferedOutputStream bos = new BufferedOutputStream( System.out,
																 1 << 24 );
			long sz = 1L << log2size;
			byte[] bs = new byte[1];
			for( long i = 1; i <= sz; i++ ) {
				r.nextBytes( bs );
				bos.write( bs );
			}
			bos.close();
		} catch( IOException ioe ) {
			System.err.println( ioe );
		}
	}
}
		
 // eof
