package edu.uw.apl.tupelo.amqp.objects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class FileHashResponse implements java.io.Serializable {

	public FileHashResponse( String algorithm ) {
		this.algorithm = algorithm;
		hits = new ArrayList<Hit>();
	}

	public void add( byte[] hash, ManagedDiskDescriptor mdd, String path ) {
		Hit h = new Hit( hash, mdd, path );
		hits.add( h );
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

		public final byte[] hash;
		public final ManagedDiskDescriptor descriptor;
		public final String path;
	}

	// Intended to be accessed by amqp.client, amqp,server code only...
	public final String algorithm;
	public final List<Hit> hits;
	
}

// eof


