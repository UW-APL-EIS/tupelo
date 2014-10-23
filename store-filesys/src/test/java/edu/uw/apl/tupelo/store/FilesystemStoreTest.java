package edu.uw.apl.tupelo.store.filesys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class FilesystemStoreTest extends junit.framework.TestCase {

	FilesystemStore store;
	
	protected void setUp() {
		store = new FilesystemStore( new File( "test-store" ), false );
	}
	
	public void testNull() {
	}

	public void testDuplicatePut() throws Exception {
		File f = new File( "src/test/resources/1m" );
		if( !f.exists() )
			return;
		DiskImage di = new DiskImage( f );
		Session session = Session.CANNED;
		FlatDisk fd = new FlatDisk( di, session );
		store.put( fd );

		try {
			store.put( fd );
			fail();
		} catch( IllegalArgumentException iae ) {
		}
	}
	
	/*
	  public void test1() throws Exception {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		assertEquals( mdds.size(), 0 );
		System.out.println( mdds );

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
	*/

	public void testAttributeRoundTest() throws Exception {

		Session session = Session.CANNED;
		String diskID = "someDisk";
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( diskID, session );
		String key = "foo";
		byte[] value1 = "Tupelo".getBytes();

		store.setAttribute( mdd, key, value1 );

		byte[] value2 = store.getAttribute( mdd, key );

		assertTrue( Arrays.equals( value1, value2 ) );
	}

}

// eof
