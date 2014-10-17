package edu.uw.apl.tupelo.cli;

import java.io.IOException;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.store.Store;

/**
 * Various routines shared by most/all cli sample programs
 */

public class Utils {

	static public ManagedDiskDescriptor locateDescriptor( Store s, String diskID,
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
}

// eof
