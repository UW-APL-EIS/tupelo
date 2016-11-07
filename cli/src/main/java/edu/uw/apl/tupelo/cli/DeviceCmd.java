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
package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.RandomDisk;
import edu.uw.apl.tupelo.model.ZeroDisk;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.PhysicalDisk;
import edu.uw.apl.tupelo.model.VirtualDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.config.Config;

import edu.uw.apl.commons.devicefiles.DeviceFile;

public class DeviceCmd extends Command {

	Command.Lambda LIST = new Command.Lambda() {
			public void invoke( Config c, boolean verbose,
								CommandLine cl ) throws Exception {
				list( c, verbose, cl );
			}
		};
	Command.Lambda ADD = new Command.Lambda() {
			public void invoke( Config c, boolean verbose,
								CommandLine cl ) throws Exception {
				add( c, verbose, cl );
			}
		};
	Command.Lambda REMOVE = new Command.Lambda() {
			public void invoke( Config c, boolean verbose,
								CommandLine cl ) throws Exception {
				remove( c, verbose, cl );
			}
		};
	
	DeviceCmd() {
		super( "device" );

		Options osAdd = new Options();
		Option oa1 = new Option( "i", true,
								 "AlternativeID (device name unavailable)" );
		oa1.setArgName( "alternateID" );
		osAdd.addOption( oa1 );
		addSub( "add", ADD, osAdd, "name", "path" );

		Options osRemove = new Options();
		addSub( "remove", REMOVE, osRemove, "name" );

		Options osList = new Options();
		addSub( "list", LIST, osList );
	}

	private void list( Config c, boolean verbose, CommandLine cl ) {
		List<Config.Device> ds = c.devices();
		for( Config.Device d : ds ) {
			System.out.println( d.getName() );
			System.out.println( " path = " + d.getPath() );
			System.out.println( " id   = " + d.getID() );
			System.out.println( " size = " + d.getSize() );
		}
	}

	private void add( Config c, boolean verbose, CommandLine cl )
		throws Exception {

		// Known we have 2 args, no need to check (see Main)
		String[] args = cl.getArgs();
		String name = args[0];
		String path = args[1];


		if( cl.hasOption( "i" ) ) {
			name = cl.getOptionValue( "i" );
		}

		System.out.println( name + " => " + path );

		UnmanagedDisk ud = null;
		if( false ) {
		} else if( path.equals( "random" ) ) {
			long log2size = 30L;
			if( args.length > 2 ) {
				try {
					log2size = Long.parseLong( args[2] );
				} catch( NumberFormatException nfe ) {
				}
			} else {
				System.out.println( "Using log2size: " + log2size );
			}
			long readSpeed = 100 * (1L << 20);
			long size = 1 << log2size;
			RandomDisk rd = new RandomDisk( size, readSpeed );
			ud = rd;
		} else if( path.equals( "zero" ) ) {
			long log2size = 30L;
			if( args.length > 2 ) {
				try {
					log2size = Long.parseLong( args[2] );
				} catch( NumberFormatException nfe ) {
				}
			} else {
				System.out.println( "Using log2size: " + log2size );
			}
			long readSpeed = 100 * (1L << 20);
			long size = 1 << log2size;
			ZeroDisk zd = new ZeroDisk( size, readSpeed );
			ud = zd;
		} else if( path.startsWith( "/dev/" ) ) {
			File f = new File( path );
			PhysicalDisk pd = new PhysicalDisk( f );
			ud = pd;
		} else if( path.endsWith( ".dd" ) ) {
			File f = new File( path );
			DiskImage di = new DiskImage( f );
			ud = di;
		} else if( VirtualDisk.likelyVirtualDisk( new File(path) ) ) {
			File f = new File( path );
			VirtualDisk vd = new VirtualDisk( f );
			ud = vd;
		}
		Config.Device d = c.addDevice( name, path );
		if( ud != null ) {
			d.setID( ud.getID() );
			d.setSize( ud.size() );
			c.store();
		} else {
			System.err.println( path + ": cannot process" );
		}
	}

	private void remove( Config c, boolean verbose, CommandLine cl )
		throws Exception {

		String[] args = cl.getArgs();
		String name = args[0];
		c.removeDevice( name );
		c.store();
	}
}

// eof
