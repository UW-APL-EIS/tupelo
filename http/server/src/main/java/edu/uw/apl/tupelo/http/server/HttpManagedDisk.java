package edu.uw.apl.tupelo.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.SeekableInputStream;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;


/**
   A ManagedDisk as can be re-constituted by a servlet from components
   passed in e.g. a http request pathInfo (for diskId and sessionID)
   and inputStream (for the content).  Effectively an unmarshalling
   operation.  A nice fallout of this way of 'uploading' a ManagedDisk
   over http is that there is no requirement that the client is Java.

   This odd class is needed by the {@link DataServlet} since it has to
   pass ManagedDisk objects to/from the Store.  The Store of course
   has no idea that http is involved at all.
*/

public class HttpManagedDisk extends ManagedDisk {

	/**
	 * @param diskID - will come from e.g. the pathInfo of the http request
	 * @param session - will come from e.g. the pathInfo of the http request,
	 * and thus be parsed into an object
	 */
	HttpManagedDisk( ManagedDiskDescriptor mdd,
					 InputStream httpRequestContent ) {
		super( null, null );
		descriptor = mdd;
		is = httpRequestContent;
	}

	@Override
	public ManagedDiskDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public void reportMetaData() {
	}

	@Override
	public void setParentDigest( List<byte[]> hashes ) {
	}
	
	@Override
	public void setParent( ManagedDisk md ) {
		throw new IllegalStateException( getClass() + ".setParent!!" );
	}

	@Override
	public void writeTo( OutputStream os ) throws IOException {
		/*
		  Note how there is NO header to write, not as an object
		  anyway.  The http client is charged with building the header
		  AND content and streaming the entire 'ManagedDisk' object to
		  us as a stream.  All we do is provide a bridge to the Store.
		*/
		IOUtils.copyLarge( is, os );
	}

	@Override
	public void readFromWriteTo( InputStream is, OutputStream os ) {
		throw new UnsupportedOperationException( getClass() +
												 ".readFromWriteTo" );
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException( getClass() +
												 ".getInputStream!!" );
	}

	/**
	 * An HttpManagedDisk just sees a byte stream from the http
	 * request (POST) of the client side ManagedDisk put.  It does NOT
	 * know which type of object was being pushed, FlatDisk,
	 * StreamOptimizedDisk, etc.  So how can we verify here that the
	 * upload actually worked?  Well, after a store put, load the
	 * managedData and create a delegate ManagedDisk and have THAT do
	 * the verify. The static ManagedDisk.readFrom routine does the
	 * work of creating the correct ManagedDisk impl, so here we never
	 * even need to know it.
	 */
	 
	@Override
	public void verify() throws IOException {
		if( managedData == null )
			throw new IllegalStateException( "Verify failed. noManagedData" );
		ManagedDisk delegate = ManagedDisk.readFrom( managedData );
		delegate.verify();
	}

	@Override
	public SeekableInputStream getSeekableInputStream() throws IOException {
		throw new UnsupportedOperationException( getClass() +
												 ".getSeekableInputStream!!");
	}

	private final ManagedDiskDescriptor descriptor;
	private final InputStream is;
}

// eof
