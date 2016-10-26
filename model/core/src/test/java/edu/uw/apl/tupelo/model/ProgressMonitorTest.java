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
package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.output.NullOutputStream;

/**
 * Testing ProgressMonitor class.  We use a NullOutputStream for all
 * 'stores' so do not pay any cost on disk of monitoring various
 * 'unmanaged to managed' transfers.

 * A simple ProgressMonitor.Callback impl just prints out progress,
 * including percent complete. Asserts are also made that the in byte
 * count seen by the monitor eventually equals the unmanaged disk
 * size.
 */

public class ProgressMonitorTest extends junit.framework.TestCase {

	class ManagedDiskMonitor implements ProgressMonitor.Callback {
		ManagedDiskMonitor( ManagedDisk md ) {
			this.md = md;
		}
		@Override
		public void update( long in, long out, long elapsed ) {
			total = in;
			double pc = (double)total / md.size() * 100;
			System.out.println( in + " " + out + " " + pc + " " + elapsed );
		}
		ManagedDisk md;
		long total;
	}
	
	
	public void testNull() {
	}

	public void testMonitor64k() throws IOException {
		File f = new File( "src/test/resources/64kXXX" );
		if( !f.exists() )
			return;
		System.out.println( f );
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		NullOutputStream nos = new NullOutputStream();
		ManagedDiskMonitor mdm = new ManagedDiskMonitor( fd );
		ProgressMonitor pme = new ProgressMonitor( fd, nos, mdm, 1 );
		pme.start();
	}

	public void testMonitor1m() throws IOException {
		File f = new File( "src/test/resources/1mXXX" );
		if( !f.exists() )
			return;
		System.out.println( f );
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		NullOutputStream nos = new NullOutputStream();
		ManagedDiskMonitor mdm = new ManagedDiskMonitor( fd );
		ProgressMonitor pme = new ProgressMonitor( fd, nos, mdm, 1 );
		pme.start();
	}

	public void testMonitor32m() throws IOException {
		File f = new File( "src/test/resources/32m.zeroXXX" );
		if( !f.exists() )
			return;
		System.out.println( f );
		UnmanagedDisk ud = new DiskImage( f );
		FlatDisk fd = new FlatDisk( ud, Session.CANNED );
		NullOutputStream nos = new NullOutputStream();
		ManagedDiskMonitor mdm = new ManagedDiskMonitor( fd );
		ProgressMonitor pme = new ProgressMonitor( fd, nos, mdm, 1 );
		pme.start();
	}

	public void testMonitor1gFlat() throws IOException {
		// Avoid placing in src/test/resources, since surefire makes COPIES!
		File f = new File( "./1g.zero" );
		testMonitorFlat( f );
	}
	
	private void testMonitorFlat( File f ) throws IOException {
		if( !f.exists() )
			return;
		
		/*
		  LOOK: why are we doing this over and over? Looking
		  for interactions (on disk?) between runs??
		*/
		int N = 5;

		for( int i = 1; i <= N; i++ ) {
			System.out.println( "testMonitorFlat : " + f + " " + i );
			UnmanagedDisk ud = new DiskImage( f );
			FlatDisk fd = new FlatDisk( ud, Session.CANNED );
			NullOutputStream nos = new NullOutputStream();
			ManagedDiskMonitor mdm = new ManagedDiskMonitor( fd );
			ProgressMonitor pme = new ProgressMonitor( fd, nos, mdm, 5 );
			pme.start();
			assertTrue( mdm.total == ud.size() );
		}
	}

	public void testMonitor1gStreamOptimized() throws IOException {
		// Avoid placing in src/test/resources, since surefire makes COPIES!
		File f = new File( "./1g" );
		testMonitorStreamOptimized( f );
	}
	
	public void testMonitor4gStreamOptimized() throws IOException {
		// Avoid placing in src/test/resources, since surefire makes COPIES!
		File f = new File( "./4g" );
		testMonitorStreamOptimized( f );
	}

	private void testMonitorStreamOptimized( File f ) throws IOException {
		if( !f.exists() )
			return;

		/*
		  LOOK: why are we doing this over and over? Looking
		  for interactions (on disk?) between runs??
		*/
		int N = 5;

		for( int i = 1; i <= N; i++ ) {
			System.out.println( "testMonitorStreamOptimized : " + f + " " + i );
			UnmanagedDisk ud = new DiskImage( f );
			StreamOptimizedDisk sod = new StreamOptimizedDisk( ud,
															   Session.CANNED );
			sod.setCompression( ManagedDisk.Compressions.SNAPPY );
			ManagedDiskMonitor mdm = new ManagedDiskMonitor( sod );
			NullOutputStream nos = new NullOutputStream();
			ProgressMonitor pme = new ProgressMonitor( sod, nos, mdm, 5 );
			pme.start();
			nos.close();
			assertTrue( mdm.total == ud.size() );
		}
	}
}

// eof
