package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.output.NullOutputStream;

public class ProgressMonitorEnablerTest extends junit.framework.TestCase {
	
	public void testNull() {
	}

	public void testMonitor64k() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		NullOutputStream nos = new NullOutputStream();
		ProgressMonitorEnabler pme = new ProgressMonitorEnabler
			( null, fd, nos );
		pme.start();
	}

	public void testMonitor1m() throws IOException {
		File f = new File( "src/test/resources/1m" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		NullOutputStream nos = new NullOutputStream();
		ProgressMonitorEnabler pme = new ProgressMonitorEnabler
			( null, fd, nos );
		pme.start();
	}

	public void testMonitor32m() throws IOException {
		File f = new File( "src/test/resources/32m.zero" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		NullOutputStream nos = new NullOutputStream();
		ProgressMonitorEnabler pme = new ProgressMonitorEnabler
			( null, fd, nos );
		pme.start();
	}

	public void testMonitor1g() throws IOException {
		File f = new File( "src/test/resources/1g.zero" );
		if( !f.exists() )
			return;
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		NullOutputStream nos = new NullOutputStream();
		ProgressMonitorEnabler pme = new ProgressMonitorEnabler
			( null, fd, nos );
		pme.start();
	}
}

// eof
