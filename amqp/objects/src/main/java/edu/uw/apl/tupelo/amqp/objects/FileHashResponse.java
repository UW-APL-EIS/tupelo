package edu.uw.apl.tupelo.amqp.objects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

/**
 * For transmission, a FileHashResponse is always wrapped in an
 * RPCObject, and so goes on the amqp bus as JSON-encoded text:
 *
 {"appdata":{"algorithm":"md5","hits":[{"hash":"0000000000000000000000000000000000000000","descriptor":{"diskID":"seagate1234","session":"5daad27e-97f2-4cb7-9b42-5ff024169963/20141104.0016"},"path":"/"}]},"name":"edu.uw.apl.tupelo.amqp.objects.FileHashResponse","hostname":"rejewski","protocolver":null,"release":null,"platform":"Linux rejewski 3.2.0-68-generic #102-Ubuntu SMP Tue Aug 12 22:02:15 UTC 2014 x86_64 x86_64 x86_64 GNU/Linux","pid":0,"time":1415139031}
 *
 * @see RPCObject for how to wrap, unwrap FileHashResponse instances. Note
 * how locally we hold no RPCObject knowledge at all.
 *
 * @see FileHashResponseTest
*/

import org.apache.commons.codec.binary.Hex;

public class FileHashResponse {

	public FileHashResponse( String algorithm ) {
		this.algorithm = algorithm;
		hits = new ArrayList<Hit>();
	}

	public void add( byte[] hash, ManagedDiskDescriptor mdd, String path ) {
		Hit h = new Hit( hash, mdd, path );
		hits.add( h );
	}

	public String paramString() {
		return algorithm + " " + hits;
	}
	
	/**
	 * A hash search 'hit': We found 'hash' on ManagedDisk
	 * mdd (diskID,session), with exact filesystem location 'path'
	 */
	   
	static public class Hit {
		
		Hit( byte[] hash, ManagedDiskDescriptor mdd, String path ) {
			this.hash = hash;
			this.descriptor = mdd;
			this.path = path;
		}

		@Override
		public String toString() {
			String hashHex = new String( Hex.encodeHex( hash ) );
			return "Hash: "+ hashHex + ", descriptor" + descriptor +
				", path: " + path;
		}
		
		public final byte[] hash;
		public final ManagedDiskDescriptor descriptor;
		public final String path;
	}

	// Intended to be accessed by amqp.client, amqp,server code only...
	public final String algorithm;
	public final List<Hit> hits;
}

// eof


