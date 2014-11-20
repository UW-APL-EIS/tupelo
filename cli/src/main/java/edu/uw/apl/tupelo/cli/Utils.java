package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.store.null_.NullStore;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;

/**
 * Various routines shared by most/all cli sample programs
 */

public class Utils {

	static final String STORELOCATIONDEFAULT = "./test-store";

	static public Options commonOptions() {
		Options os = new Options();
		os.addOption( "d", false, "Debug" );
		os.addOption( "h", false, "Help" );
		os.addOption( "s", true,
					  "Store url/directory. Defaults to " +
					  STORELOCATIONDEFAULT );
		return os;
	}
	
	static public ManagedDiskDescriptor locateDescriptor( Store s,
														  String diskID,
														  String sessionID )
		throws IOException {
		for( ManagedDiskDescriptor mdd : s.enumerate() ) {
			if( mdd.getDiskID().equals( diskID ) &&
				mdd.getSession().toString().equals( sessionID ) ) {
				return mdd;
			}
		}
		return null;
	}

	static public Store buildStore( String storeLocation ) {
		Store s = null;
		if( false ) {
		} else if( storeLocation.equals( "/dev/null" ) ) {
			s = new NullStore();
		} else if( storeLocation.startsWith( "http" ) ) {
			s = new HttpStoreProxy( storeLocation );
		} else {
			File dir = new File( storeLocation );
			if( !dir.isDirectory() ) {
				throw new IllegalStateException
					( "Not a directory: " + storeLocation );
			}
			s = new FilesystemStore( dir );
		}
		return s;
	}

}

// eof
