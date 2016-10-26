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

/**
 * Rather than do div and mod arithmetic to locate gdIndex, gtIndex
 * and gOffset for any given 'file offset', use the fact that
 * grainSize and grainTableSize are powers of 2.  Then, div and mod
 * can be replaced by shifts and masks.  Validate this logic
 */
public class GrainArithmeticTest extends junit.framework.TestCase {

	public void test32m() {
		test( 1024L * 1024L * 32, 128, 512 );
	}

	public void _test1g() {
		test( 1024L * 1024L * 1024L, 128, 512 );
	}

	public void _test6g() {
		test( 1024L * 1024L * 1024L * 6, 128, 512 );
	}

	public void _test128g() {
		test( 1024L * 1024L * 1024L * 128L, 128, 512 );
	}

	private void test( long limit, int grainSize, int numGTEsPerGT ) {
		long grainSizeBytes = grainSize * Constants.SECTORLENGTH;
		int log2GrainSize = log2( grainSizeBytes );
		
		long grainTableSizeBytes = grainSizeBytes * numGTEsPerGT;
		int log2GrainTableSize = log2( grainTableSizeBytes );
		
		for( long l = 0; l < limit; l++ ) {
			long gdIndex = l >>> log2GrainTableSize;
			long inTable = l - (gdIndex << log2GrainTableSize);
			long gtIndex = inTable >>> log2GrainSize;
			long gOffset = inTable & (grainSizeBytes - 1);
			long l2 = (gdIndex << log2GrainTableSize) +
				(gtIndex << log2GrainSize) + gOffset;
			if( false ) {
				System.out.println( l + " " + gdIndex + " " + gtIndex + " " +
									gOffset + " " + l2 );
			}
			assertEquals( l, l2 );
		}
	}

	static int log2( long i ) {
		for( int p = 0; p < 32; p++ ) {
			if( i == 1 << p )
				return p;
		}
		throw new IllegalArgumentException( "Not a pow2: " + i );
	}
}

// eof
