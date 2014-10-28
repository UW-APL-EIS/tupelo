package edu.uw.apl.tupelo.http.client;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.Session;

public class HttpStoreProxyTest extends junit.framework.TestCase {

	Store store;
	
	protected void setUp() {
		store = new HttpStoreProxy( "http://localhost:8888/tupelo" );
	}
	
	public void testNull() {
	}

	public void testUUID() throws IOException {
		UUID u = store.getUUID();
		System.out.println( "UUID: " + u );
		assertNotNull( u );
	}

	public void testUsableSpace() throws IOException {
		long us = store.getUsableSpace();
		System.out.println( "Usablespace: " + us );
	}

	public void testNewSession() throws IOException {
		Session s1 = store.newSession();
		System.out.println( "Session1: " + s1 );
		assertNotNull( s1 );

		Session s2 = store.newSession();
		System.out.println( "Session2: " + s2 );
		assertNotNull( s1 );

		// wot, no assertNotEquals
		if( s1.equals( s2 ) )
			fail();
	}

	public void testPutData() throws IOException {
		File f = new File( "32m" );
		if( !f.exists() )
			return;
		DiskImage di = new DiskImage( f );

		Session s = store.newSession();
		FlatDisk fd = new FlatDisk( di, s );
		store.put( fd );
	}

	public void testEnumerate() throws IOException {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		assertNotNull( mdds );
		System.out.println( "Enumerate: " + mdds );
	}
}

// eof
