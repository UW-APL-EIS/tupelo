package edu.uw.apl.tupelo.store.filesys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.Session;

public class DigestComputeTest extends junit.framework.TestCase {

	FilesystemStore store;
	
	protected void setUp() {
		store = new FilesystemStore( new File( "test-store" ) );
	}
	
	public void testNull() {
	}

	public void testDigests() throws IOException {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		for( ManagedDiskDescriptor mdd : mdds ) {
			System.out.println( "Digesting: " + mdd );
			ManagedDiskDigest d = store.digest( mdd );
			System.out.println( d );
		}
	}
}

// eof
