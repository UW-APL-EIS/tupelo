package edu.uw.apl.tupelo.model;


import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used for monitoring how ManagedDisk 'transfer' operations are
 * proceeding.  By transfer we mean the act of transferring unmanaged
 * data (a DiskImage, PhysicalDisk, VirtualDisk) to managed data,
 * normally by way of a Store.put() operation.
 *
 * Monitoring the progress of this transfer gives us a way to print
 * the familiar "X % finished" to the user, keeping them informed of
 * possibly lengthy (minutes, hours??) operations.
 *
 * A nice feature of this design is that the ManagedDisk
 * implementations do not have to 'participate' in the byte counting
 * involved in the transfer monitoring process.  Given how complex
 * e.g. {@link StreamOptimizedDisk} 'transfers' (unmananged to
 * managed) are, this is a VERY good thing.  All that ManagedDisk
 * implementations have to do is implement {@link #readFromWriteTo},
 * and use the supplied InputStream to read unmanaaged data from, and
 * not their own unmanagedData.getInputStream().
 */

public class ProgressMonitor {

	/**
	   This callback is what the 'user' supplies, so they get to
	   decide 'how' to inform (display) the user of progress. The
	   ProgressMonitor just supplies the numbers (bytes
	   read,written,time elapsed)
	*/
	
	public interface Callback {
		/**
		 * @param elapsed - seconds since the operation started, e.g. a
		 * store put
		 */
		public void update( long unmanagedBytesRead,
							long managedBytesWritten, long elapsed );
	}
	
	public ProgressMonitor( ManagedDisk md, OutputStream os,
							Callback cb, int updateIntervalSecs )
		throws IOException {
		this.md = md;
		InputStream is = md.getUnmanaged().getInputStream();
		//		BufferedInputStream bis = new BufferedInputStream( is, 1024*64 );
		cis = new CountingInputStream( is );
		cos = new CountingOutputStream( os );
		this.cb = cb;
		this.updateIntervalSecs = updateIntervalSecs;
		log = LogFactory.getLog( getClass() );
	}

	public void start() throws IOException {
		Runnable r = new Runnable() {
				public void run() {
					long start = System.currentTimeMillis();
					while( true ) {
						try {
							Thread.sleep( updateIntervalSecs * 1000L );
						} catch( InterruptedException ie ) {
							break;
						}
						/*
						  Even with done from a sleep interrupt,
						  continue with one last callback update, else
						  the user may never see '100% done'
						*/
						long in = cis.getByteCount();
						long out = cos.getByteCount();
						long now = System.currentTimeMillis();
						long elapsed = now - start;
						cb.update( in, out, elapsed / 1000 );
						if( in == md.size() )
							break;
					}
				}
			};
		t = new Thread( r );
		t.start();
		try {
			md.readFromWriteTo( cis, cos );
		} catch( Exception e ) {
			log.warn( e );
			t.interrupt();
		} finally {
			/*
			  Even if the readFromWriteTo blows up, we need to clean up
			  especially joining the byte counting thread
			*/
			cis.close();
			cos.close();
			try {
				t.join();
			} catch( InterruptedException ie ) {
			}
		}
	}

	public void interrupt() {
		t.interrupt();
	}
	
	ManagedDisk md;
	CountingInputStream cis;
	CountingOutputStream cos;
	Callback cb;
	int updateIntervalSecs;
	Thread t;
	Log log;
}

// eof

