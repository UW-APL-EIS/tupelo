package edu.uw.apl.tupelo.model;

import java.io.DataInput;
import java.io.IOException;

public class GrainTable {

	public GrainTable( DataInput di, int len ) throws IOException {
		gtes = new long[len];
		for( int i = 0; i < gtes.length; i++ )
			gtes[i] = di.readInt() & 0xffffffffL;
	}

	final long[] gtes;

	// Stored in a managed disk file as a gde == 0
	//	static final GrainTable ZERO = new GrainTable( null, 0 );

	// Stored in a managed disk file as a gde == -1
	//static final GrainTable PARENT = new GrainTable( null, 0 );
}

// eof

