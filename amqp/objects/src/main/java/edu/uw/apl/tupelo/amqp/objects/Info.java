package edu.uw.apl.tupelo.amqp.objects;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Hex;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

/**
 * A supplementary tool for this package, which we run (we have a
 * main) simply prints to stdout the json strings for the all the
 * possible objects comprising the 'Tupelo amqp message set'
 */
public class Info {

	public static void main( String[] unused ) {

		Gson gson = Utils.createGson( true );
		showFileHashQuery( gson );
		showFileHashResponse( gson );
	}

	static void showFileHashQuery( Gson gson ) {
		FileHashQuery fhq = new FileHashQuery( "md5" );
		byte[] hash1 = "12345678901234567890".getBytes();
		fhq.add( hash1 );
		byte[] hash2 = "ABCDEFGHIJKLMNOPQRST".getBytes();
		fhq.add( hash2 );

		RPCObject<FileHashQuery> fhqRPC = RPCObject.asRPCObject( fhq );
		String fhqs = gson.toJson( fhqRPC );

		System.out.println();
		System.out.println
			( "A 'file hash query' is sent by a Tupelo amqp client." );
		System.out.println( "It is received by a Tupelo amqp service." );
		System.out.println();
		System.out.println
			( "On the wire, a file hash query looks like this: ");
		System.out.println();
		System.out.println( fhqs );
		System.out.println();

	}

	static void showFileHashResponse( Gson gson ) {

		FileHashResponse fhr = new FileHashResponse( "md5" );
		byte[] hash1 = "12345678901234567890".getBytes();
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor
			( "someDiskID", Session.testSession() );
		String path = "/path/to/the/located/file";
		fhr.add( hash1, mdd, path );

		RPCObject<FileHashResponse> fhrRPC = RPCObject.asRPCObject( fhr );
		String fhrs = gson.toJson( fhrRPC );

		System.out.println();
		System.out.println
			( "A 'file hash response' is sent by a Tupelo amqp service." );
		System.out.println( "It is received by a Tupelo amqp client." );
		System.out.println();
		System.out.println
			( "On the wire, a file hash response looks like this: ");
		System.out.println();
		System.out.println( fhrs );
	}
}

// eof
