package edu.uw.apl.tupelo.http.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Before;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.Constants;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.Session;

/**
 * In order to run these http client-side tests, we need the
 * corresponding server-side up.  So, in a separate terminal:
 *
 * $ cd /path/to/tupelo/http/server
 * $ mvn jetty:run
 *
 * which should start the Tupelo Http Server (in the Jetty web
 * container) on port 8888/tcp.
 */

public class HttpStoreProxyTest {//extends junit.framework.TestCase {

	static Server server;

	static String serverHome =
		"/home/stuart/apl/projects/infosec/dims/tupelo/http/server";
	
	Store store;

	@BeforeClass
	public static void setServer() throws Exception {
		if( true )
			return;
		server = new Server( 8888 );
		server.setStopAtShutdown( true );
		WebAppContext wac = new WebAppContext();
		wac.setContextPath( "/tupelo" );
		wac.setWar( serverHome + "/target/tupelo-http-server-0.0.1.war" );
		/*
		  wac.setResourceBase( serverHome + "/src/main/webapp" );
		URL servlets = new URL( "file://" + serverHome + "/target/classes/" );
		URLClassLoader ucl = new URLClassLoader( new URL[] { servlets } );
		*/
		//wac.setClassLoader( ucl );
		server.setHandler( wac );
		server.start();
	}

	@Before
	public void buildStore() {
		store = new HttpStoreProxy( "http://localhost:8888/tupelo" );
		System.out.println( store );
	}

	@Test
	public void testNull() {
	}

	@Test
	public void testUUID() throws IOException {
		UUID u = store.getUUID();
		System.out.println( "UUID: " + u );
		assertNotNull( u );
	}

	@Test
	public void testUsableSpace() throws IOException {
		long us = store.getUsableSpace();
		System.out.println( "Usablespace: " + us );
	}

	//	@Test
	@Ignore
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

	@Ignore
	public void testPutData() throws IOException {
		File f = new File( "32m" );
		if( !f.exists() )
			return;
		DiskImage di = new DiskImage( f );

		Session s = store.newSession();
		FlatDisk fd = new FlatDisk( di, s );
		store.put( fd );
	}

	@Ignore
	public void testDigest() throws IOException {
		File f = new File( "64m" );
		if( !f.exists() )
			return;
		DiskImage di = new DiskImage( f );
		Session s = store.newSession();
		FlatDisk fd = new FlatDisk( di, s );
		store.put( fd );

		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( di.getID(), s );
		List<byte[]> digest = store.digest( mdd );
		long grainSizeBytes = ManagedDisk.GRAINSIZE_DEFAULT *
			Constants.SECTORLENGTH;
		assertEquals( digest.size(), f.length() / grainSizeBytes );
	}

	@Test
	public void testEnumerate() throws IOException {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		assertNotNull( mdds );
		System.out.println( "Enumerate: " + mdds );
	}

	@Test
	public void testSizeRequest() throws IOException {
		System.out.println( "testSizeRequest" );
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		assertNotNull( mdds );
		for( ManagedDiskDescriptor mdd : mdds ) {
			long size = store.size( mdd );
			System.out.println( "Size: " + mdd + " = " + size );
		}
	}

	@Test
	public void testAttributeList() throws IOException {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		assertNotNull( mdds );
		if( mdds.isEmpty() )
			return;
		List<ManagedDiskDescriptor> asList = new ArrayList<ManagedDiskDescriptor>
			( mdds );
		ManagedDiskDescriptor mdd = asList.get(0);
		Collection<String> ss = store.listAttributes( mdd );
		System.out.println( ss );
	}

	@Test
	public void testRoundTripAttribute() throws IOException {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		assertNotNull( mdds );
		if( mdds.isEmpty() )
			return;
		List<ManagedDiskDescriptor> asList = new ArrayList<ManagedDiskDescriptor>
			( mdds );
		ManagedDiskDescriptor mdd = asList.get(0);
		String key = "hello";
		byte[] value = "world".getBytes();
		store.setAttribute( mdd, key, value );

		byte[] value2 = store.getAttribute( mdd, key );
		String s2 = new String( value2 );
		assertEquals( s2, "world" );
	}
}

// eof
