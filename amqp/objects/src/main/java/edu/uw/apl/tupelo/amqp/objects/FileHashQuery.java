package edu.uw.apl.tupelo.amqp.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * For transmission, a FileHashQuery is always wrapped in an
 * RPCObject, and so goes on the amqp bus as JSON-encoded text:
 *
 * {"appdata":{"algorithm":"sha1","hashes":["0000000000000000000000000000000000000000","0000000000000000000000000000000000000000"]},"name":"filehashquery","hostname":"rejewski","protocolver":null,"release":null,"platform":"Linux rejewski 3.2.0-68-generic #102-Ubuntu SMP Tue Aug 12 22:02:15 UTC 2014 x86_64 x86_64 x86_64 GNU/Linux","pid":0,"time":1415134659}
 *
 * @see RPCObject for how to wrap, unwrap FileHashQuery instances. Note
 * how locally we hold no RPCObject knowledge at all.
 *
 * @see FileHashQueryTest
*/
public class FileHashQuery {
	
	public FileHashQuery( String algorithm ) {
		this.algorithm = algorithm;
		hashes = new ArrayList<byte[]>();
	}

	public void add( byte[] hash ) {
		hashes.add( hash );
	}

	// Intended to be accessed by amqp.client, amqp.server code only...
	public final String algorithm;
	public final List<byte[]> hashes;
}

// eof


