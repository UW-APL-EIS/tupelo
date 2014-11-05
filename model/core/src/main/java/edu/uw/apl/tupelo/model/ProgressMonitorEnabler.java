package edu.uw.apl.tupelo.model;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;

public class ProgressMonitorEnabler {

	ProgressMonitorEnabler( ProgressMonitor pm, ManagedDisk md,
							OutputStream os ) throws IOException {
		this.md = md;
		this.is = new CountingInputStream( md.getUnmanaged().getInputStream() );
		this.os = new CountingOutputStream( os );
		this.pm = pm;
	}

	/*
	public InputStream getInputStream() throws IOException {
		return is;
	}

	public OutputStream getOutputStream() throws IOException {
		return os;
	}
	*/

	public void start() throws IOException {
		Runnable r = new Runnable() {
				public void run() {
					while( true ) {
						System.out.println( "PME" );
						try {
							Thread.sleep( 1000 * 2 );
						} catch( InterruptedException ie ) {
							break;
						}
						long in = is.getByteCount();
						long out = os.getByteCount();
						System.out.println( in + " " + out );
						if( in == md.size() )
							break;
					}
				}
			};
		Thread t = new Thread( r );
		t.start();
		md.readFromWriteTo( is, os );
	}
		
	ManagedDisk md;
	CountingInputStream is;
	CountingOutputStream os;
	ProgressMonitor pm;
}

// eof
