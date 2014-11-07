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
