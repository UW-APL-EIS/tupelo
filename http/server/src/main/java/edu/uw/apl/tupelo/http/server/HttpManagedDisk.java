/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uw.apl.tupelo.http.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.SeekableInputStream;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;


/**
   A ManagedDisk as can be re-constituted by a servlet from components
   passed in e.g. an http request pathInfo (for diskId and sessionID)
   and inputStream (for the content).  Effectively an unmarshalling
   operation.  A nice fallout of this way of 'uploading' a ManagedDisk
   over http is that there is no requirement that the client is Java.

   This odd class is needed by the {@link DataServlet} since it has to
   pass ManagedDisk objects to/from the Store.  The Store of course
   has no idea that http is involved at all.

   Once into the store, the HttpManagedDisk will have its
   setManagedData() call applied, just like any managedDisk does.
   Unlike other managedDisk types though, this causes the
   HttpManagedDisk to create its 'delegate' ManagedDisk object. All
   subsequent store-derived calls into an HttpManagedDisk are then
   forwarded to the delegate. The nice thing about this design is that
   the store need never know it is dealing with http-uploaded data.
   The alternative to this 'delegate tracking' would be for all
   ManagedDisk types to provide a 'morph into' api, this is better.
   Of course when the store is restarted, those objects that were
   HttpManagedDisks at time of previous upload would now be loaded
   from store disk as e.g. StreamOptimizedDisks.  Thus
   HttpManagedDisks are only needed as objects in the time interval
   between their creation and the next store restart.
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
	public ManagedDiskDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public void setManagedData( File f ) {
		try {
			delegate = ManagedDisk.readFrom( f );
		} catch( IOException ioe ) {
			log.warn( ioe );
		}
	}

	@Override
	public long size() {
		checkDelegate();
		return delegate.size();
	}

	@Override
	public UUID getUUIDCreate() {
		checkDelegate();
		return delegate.getUUIDCreate();
	}

	@Override
	public UUID getUUIDParent() {
		checkDelegate();
		return delegate.getUUIDParent();
	}

	@Override
	public long grainSizeBytes() {
		checkDelegate();
		return delegate.grainSizeBytes();
	}
	
	@Override
	public boolean hasParent() {
		checkDelegate();
		return delegate.hasParent();
	}

	@Override
	public void reportMetaData() throws IOException {
		checkDelegate();
		delegate.reportMetaData();
	}

	/*
	  This is a purely client-side operation, and a client should
	  never be creating an HttpManagedDisk
	*/
	@Override
	public void setParentDigest( ManagedDiskDigest grainHashes ) {
		throw new UnsupportedOperationException( "setParentDigest" );
	}

	
	@Override
	public void setParent( ManagedDisk md ) {
		checkDelegate();
		delegate.setParent( md );
	}

	@Override
	public InputStream getInputStream() throws IOException {
		checkDelegate();
		return delegate.getInputStream();
	}

	@Override
	public SeekableInputStream getSeekableInputStream() throws IOException {
		checkDelegate();
		return delegate.getSeekableInputStream();
	}
	
	@Override
	public void verify() throws IOException {
		checkDelegate();
		delegate.verify();
	}

	private void checkDelegate() {
		if( delegate == null )
			throw new IllegalStateException( "No delegate: " + descriptor );
	}

	private final ManagedDiskDescriptor descriptor;
	private final InputStream is;
	private ManagedDisk delegate;
}

// eof
