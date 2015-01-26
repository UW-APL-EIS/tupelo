package edu.uw.apl.tupelo.store.tools;

import java.io.IOException;
import java.util.Collection;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

/**
 * Various utilities for interrogation of a Tupelo store contents
 */

public class Utils {

	static ManagedDiskDescriptor locateDescriptor( Store s,
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
	
	static boolean isAttributePresent( Store s,
									   ManagedDiskDescriptor mdd,
									   String needle ) throws IOException {
		Collection<String> haystack = s.listAttributes( mdd );
		for( String attrName : haystack ) {
			if( attrName.equals( needle ) )
				return true;
		}
		return false;
	}
}

// eof

