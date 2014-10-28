package edu.uw.apl.tupelo.amqp.objects;

import java.util.ArrayList;
import java.util.List;

public class FileHashQuery implements java.io.Serializable {
	
	public FileHashQuery( String algorithm ) {
		this.algorithm = algorithm;
		hashes = new ArrayList<byte[]>();
	}

	public void add( byte[] hash ) {
		hashes.add( hash );
	}

	// Intended to be accessed by amqp.client, amqp,server code only...
	public final String algorithm;
	public final List<byte[]> hashes;
}

// eof


