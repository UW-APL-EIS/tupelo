package edu.uw.apl.tupelo.model;

import java.io.BufferedReader;
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

	public ManagedDiskDigest() {
		grainHashes = new ArrayList<byte[]>();
	}

	public void add( byte[] ba ) {
		byte[] copy = new byte[ba.length];
		System.arraycopy( ba, 0, copy, 0, ba.length );
		grainHashes.add( copy );
	}

	public byte[] get( int i ) {
		return grainHashes.get(i);
	}

	public int size() {
		return grainHashes.size();
	}
	
	public void writeTo( Writer w ) throws IOException {
		PrintWriter pw = new PrintWriter( w );
		for( byte[] gh : grainHashes ) {
			String hashHex = new String( Hex.encodeHex( gh ) );
			pw.println( hashHex );
		}
		pw.close();
	}

	static public ManagedDiskDigest readFrom( Reader r ) throws IOException {
		ManagedDiskDigest result = new ManagedDiskDigest();
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
