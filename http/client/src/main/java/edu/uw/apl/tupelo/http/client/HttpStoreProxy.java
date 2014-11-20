package edu.uw.apl.tupelo.http.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

//import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.store.Store;

/**
 * A client side (in the http sense that is) proxy for a Store.
 * Connects to the http.server.StoreServlet.  Kind of like an RMI
 * stub.
 */
public class HttpStoreProxy implements Store {

	public HttpStoreProxy( String s ) {
		if( !s.endsWith( "/" ) )
			s = s + "/";
		this.server = s;
		log = LogFactory.getLog( getClass() );
	}

	@Override
	public String toString() {
		// Something useful.  Do we need the class name ??
		return getClass().getName() + ":" + server;
	}
	
	@Override
	public UUID getUUID() throws IOException {
		HttpGet g = new HttpGet( server + "uuid" );
		g.addHeader( "Accept", "application/x-java-serialized-object" );
		log.debug( g.getRequestLine() );
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( g );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			UUID result = (UUID)ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}

	@Override
	public long getUsableSpace() throws IOException {
		HttpGet g = new HttpGet( server + "usablespace" );
		g.addHeader( "Accept", "application/x-java-serialized-object" );
		log.debug( g.getRequestLine() );
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( g );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			Long result = (Long)ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}

	@Override
	public Session newSession() throws IOException {
		// LOOK: is a GET good enough here?  Want non-cacheable...
		HttpPost p = new HttpPost( server + "newsession" );
		p.addHeader( "Accept", "application/x-java-serialized-object" );
	
		log.debug( p.getRequestLine() );
		
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( p );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			Session result = (Session)ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}

	@Override
	public UUID uuid( ManagedDiskDescriptor mdd ) throws IOException {
		HttpGet g = new HttpGet( server + "disks/data/uuid/" + mdd.getDiskID() +
								   "/" + mdd.getSession() );
		log.debug( g.getRequestLine() );
		g.addHeader( "Accept", "application/x-java-serialized-object" );
		log.debug( g.getRequestLine() );
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( g );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			UUID result = (UUID)ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}
	
	/**
	   Slightly awkward implementation, making use of a Pipe.  We have
	   a ManagedDisk as the source of our data, which supports just
	   writeTo( OutputStream ). But the HttpClient InputStreamEntity
	   wants to see an Inputstream.  So, in a new thread, we write the
	   ManagedDisk to the OutputStream side of the Pipe.  The caller
	   thread is then given the input side of the Pipe.
	*/
	@Override
	public void put( final ManagedDisk md ) throws IOException {
		ManagedDiskDescriptor mdd = md.getDescriptor();
		HttpPost p = new HttpPost( server + "disks/data/put/" + mdd.getDiskID() +
								   "/" + mdd.getSession() );
		log.debug( p.getRequestLine() );

		final PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream( pos );
		InputStreamEntity ise = new InputStreamEntity
			( pis, -1, ContentType.APPLICATION_OCTET_STREAM );
		ise.setChunked( true );
		p.setEntity( ise );
		Runnable r = new Runnable() {
				public void run() {
					try {
						md.writeTo( pos );
						pos.close();
					} catch( IOException ioe ) {
						log.error( ioe );
					}
				}
			};
		Thread t = new Thread( r );
		t.start();
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( p );
		try {
			t.join();
		} catch( InterruptedException ie ) {
		}
	}

	@Override
	public synchronized void put( final ManagedDisk md,
								  final ProgressMonitor.Callback cb,
								  final int progressUpdateIntervalSecs )
		throws IOException {

		ManagedDiskDescriptor mdd = md.getDescriptor();
		HttpPost p = new HttpPost( server + "disks/data/put/" +
								   mdd.getDiskID() +
								   "/" + mdd.getSession() );
		log.debug( p.getRequestLine() );

		final PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream( pos );
		InputStreamEntity ise = new InputStreamEntity
			( pis, -1, ContentType.APPLICATION_OCTET_STREAM );
		ise.setChunked( true );
		p.setEntity( ise );
		Runnable r = new Runnable() {
				public void run() {
					try {
						ProgressMonitor pm = new ProgressMonitor
							( md, pos, cb, progressUpdateIntervalSecs );
						pm.start();
						pos.close();
					} catch( IOException ioe ) {
						log.error( ioe );
					}
				}
			};
		Thread t = new Thread( r );
		t.start();
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( p );
		try {
			t.join();
		} catch( InterruptedException ie ) {
		}
	}
	

	@Override
	public List<byte[]> digest( ManagedDiskDescriptor mdd )
		throws IOException {
		HttpGet g = new HttpGet( server + "disks/data/digest/" + mdd.getDiskID() +
								 "/" + mdd.getSession() );
		g.addHeader( "Accept", "application/x-java-serialized-object" );
	
		log.debug( g.getRequestLine() );
		
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( g );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			List<byte[]>result = (List<byte[]>)ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}

		
	@Override
	public Collection<String> listAttributes( ManagedDiskDescriptor mdd )
		throws IOException {
		HttpGet g = new HttpGet( server + "disks/attr/list/" + mdd.getDiskID() +
								 "/" + mdd.getSession() );
		g.addHeader( "Accept", "application/x-java-serialized-object" );
	
		log.debug( g.getRequestLine() );
		
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( g );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			Collection<String>result = (Collection<String>)ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}

	@Override
	public byte[] getAttribute( ManagedDiskDescriptor mdd, String key )
		throws IOException {
		HttpGet g = new HttpGet( server + "disks/attr/get/" + mdd.getDiskID() +
								 "/" + mdd.getSession() + "/" + key  );
		g.addHeader( "Accept", "application/x-java-serialized-object" );
		log.debug( g.getRequestLine() );
		
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( g );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			byte[] result = (byte[])ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}

	@Override
	public void setAttribute( ManagedDiskDescriptor mdd,
							  String key, byte[] value ) throws IOException {
		HttpPost p = new HttpPost( server + "disks/attr/set/" + mdd.getDiskID() +
								   "/" + mdd.getSession() + "/" + key  );
		log.debug( p.getRequestLine() );

		ByteArrayInputStream bais = new ByteArrayInputStream( value );
		InputStreamEntity ise = new InputStreamEntity
			( bais, value.length, ContentType.APPLICATION_OCTET_STREAM );
		p.setEntity( ise );
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( p );
	}

	/*
	  For the benefit of the fuse-based ManagedDiskFileSystem, so
	  meaningless for a client-side http proxy.  Should never be
	  called.
	*/
	public ManagedDisk locate( ManagedDiskDescriptor mdd ) {
		throw new UnsupportedOperationException( "HttpStoreProxy.locate" );
	}

	@Override
	public Collection<ManagedDiskDescriptor> enumerate() throws IOException {
		HttpGet g = new HttpGet( server + "disks/data/enumerate" );
		g.addHeader( "Accept", "application/x-java-serialized-object" );
	
		log.debug( g.getRequestLine() );
		
		HttpClient req = new DefaultHttpClient( );
		HttpResponse res = req.execute( g );
		HttpEntity he = res.getEntity();
		InputStream is = he.getContent();
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			Collection<ManagedDiskDescriptor> result =
				(Collection<ManagedDiskDescriptor>)ois.readObject();
			return result;
		} catch( ClassNotFoundException cnfe ) {
			throw new IOException( cnfe );
		} finally {
			ois.close();
		}
	}

	private String server;
	private final Log log;
}

// eof
