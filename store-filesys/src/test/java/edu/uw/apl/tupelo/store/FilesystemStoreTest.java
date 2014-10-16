package edu.uw.apl.tupelo.store.filesys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class FilesystemStoreTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void test1() throws Exception {

		FilesystemStore store = new FilesystemStore( new File( "test-store" ) );
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		assertEquals( mdds.size(), 0 );
		System.out.println( mdds );

		File f = new File( "src/test/resources/1m" );
		if( !f.exists() )
			return;

		Session session = Session.CANNED;
		
		FlatDisk fd1 = new FlatDisk( f, "diskid", session );
		store.put( fd1 );
		mdds = store.enumerate();
		assertEquals( mdds.size(), 1 );
		System.out.println( mdds );

		FlatDisk fd2 = new FlatDisk( f, "diskid", session.successor() );
		store.put( fd2 );
		mdds = store.enumerate();
		assertEquals( mdds.size(), 2 );
		System.out.println( mdds );
	}
}

// eof
