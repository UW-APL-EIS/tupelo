package edu.uw.apl.tupelo.store.tools;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

/**
 * Open the identified managed disk (identified via diskID, sessionID)
 * and stream its entire contents to stdout.  Hence the name 'catmd',
 * mimics the Unix tool cat.
 *
 * Really just tests the correctness ManagedDisk.getInputStream and
 * readImpls.  Likely to be used in conjunction with other tools in a
 * pipe, e.g.
 *
 * CatMD -s someStoreURL someDiskID someSessionID | md5sum
 */

public class CatMD extends Base {

	static public void main( String[] args ) {
		CatMD main = new CatMD();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			System.exit(-1);
		} finally {
			LogManager.shutdown();
		}
			  
	}

	public CatMD() {
	}

	public void start() throws Exception {
		File dir = new File( storeLocation );
		if( !dir.isDirectory() ) {
			throw new IllegalStateException
				( "Not a directory: " + storeLocation );
		}
		FilesystemStore store = new FilesystemStore( dir );
		if( debug || verbose )
			System.out.println( "Store type: " + store );
		ManagedDiskDescriptor mdd = locateDescriptor( store,
													  diskID, sessionID );
		if( mdd == null ) {
			System.err.println( "Not stored: " + diskID + "," + sessionID );
			System.exit(1);
		}
		ManagedDisk md = store.locate( mdd );
		InputStream is = md.getInputStream();
		byte[] ba = new byte[1024*4];
		while( true ) {
			int nin = is.read( ba );
			if( nin < 1 )
				break;
			System.out.write( ba, 0, nin );
		}
	}
}

// eof
