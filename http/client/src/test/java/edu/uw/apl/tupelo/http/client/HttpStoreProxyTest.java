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
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.Session;

/**
 * These tests USED to be reliant on the Tupelo server being run in a separate terminal:
 *
 * $ cd /path/to/tupelo/http/server
 * $ mvn jetty:run
 *
 * which started the Tupelo Http Server (in the Jetty web
 * container) on port 8888/tcp.  We would then unit test this client code:
 *
 * $ cd /path/to/tupelo/http/client
 * $ mvn test
 *
 * That approach was ugly, and needed a human in the loop to manage
 * the server side.  It would not sit with with an automated build
 * environment like Jenkins. So, we now make use of a mortbay (jetty
 * folks) api for managing jetty from code, see {@link #setServer()}.
 */

public class HttpStoreProxyTest {//extends junit.framework.TestCase {

	// Main Jetty object, the web server itself...
	static Server server;

	// Configuration params for getting a Tupelo war invoked...
	static String serverModuleHome = "../server";
	static int httpPort = 8888;
	static String contextPath = "/tupelo";

	private Store store;

	@BeforeClass
	public static void setServer() throws Exception {
		server = new Server( httpPort );
		server.setStopAtShutdown( true );
		WebAppContext wac = new WebAppContext();
		wac.setContextPath( contextPath );

		// LOOK: hunting around in server module filesystem!!  Any better way ??
		File serverDir = new File( serverModuleHome );
		File warDir = new File( serverDir, "target" );
		File warFile = new File( warDir, "tupelo-http-server-0.0.1.war" );
		if( !warFile.exists() )
			warFile = new File( warDir, "tupelo.war" );
		wac.setWar( warFile.getPath() );

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
		String storeURL = "http://localhost:" + httpPort + "/" + contextPath;
		store = new HttpStoreProxy( storeURL );
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
		assertTrue( us > 0 );
	}

	@Test
	//@Ignore
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
		File f = new File( "src/test/resources/32m" );
		if( !f.exists() )
			return;
		DiskImage di = new DiskImage( f );

		Collection<ManagedDiskDescriptor> mddsPrePut = store.enumerate();
		Session s = store.newSession();
		FlatDisk fd = new FlatDisk( di, s );
		store.put( fd );

		Collection<ManagedDiskDescriptor> mddsPostPut = store.enumerate();
		assertTrue( mddsPostPut.size() == mddsPrePut.size() + 1 );
	}

	@Ignore
	public void testDigest() throws IOException {
		File f = new File( "src/test/resources/64m" );
		if( !f.exists() )
			return;
		DiskImage di = new DiskImage( f );
		Session s = store.newSession();
		FlatDisk fd = new FlatDisk( di, s );
		store.put( fd );

		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( di.getID(), s );
		ManagedDiskDigest digest = store.digest( mdd );
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
		System.out.println( "testRoundTripAttribute" );
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
