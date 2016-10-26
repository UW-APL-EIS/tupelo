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
package edu.uw.apl.tupelo.fuse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.cli.*;

import fuse.FuseMount;

import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

/**
 * A command-line entry point to creating a ManagedDiskFileSystem.
 * Takes a Tupelo store by local file name.  Various command line
 * options enable dryrun, verbose etc. To run:
 *
 * <pre>
 * java edu.uw.apl.tupelo.fuse.Main -s /path/to/tupeloStore mountPoint
 * </pre>
 *
 * where the mount point directory must exist a priori.
 * 
 */

 public class Main {


	public static void main(String[] args) throws Exception {
		Options os = new Options();
		os.addOption( "n", false,
					  "dryrun, show the filesystem but skip the mount" );
		os.addOption( "s", true,
					  "Store directory. Defaults to " + STORELOCATIONDEFAULT );
		os.addOption( "v", false, "verbose" );
		final String USAGE = Main.class.getName() + 
			" [-n] [-s storeLocation] [-v] mountPoint";
		final String HEADER = "";
		final String FOOTER = "";
		
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( Exception e ) {
			System.err.println( e );
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		String storeLocation = STORELOCATIONDEFAULT;
		boolean dryrun = cl.hasOption( "n" );
		boolean verbose = cl.hasOption( "v" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}

		args = cl.getArgs();
		if( args.length < 1 ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		
		File dir = new File( storeLocation );
		if( !dir.isDirectory() ) {
			throw new IllegalStateException
				( "Not a directory: " + storeLocation );
		}
		FilesystemStore store = new FilesystemStore( dir );
		
		File mount = new File( args[0] );
		if( !mount.isDirectory() ) {
			System.err.println( "Mount point " + mount +
								" not a directory" );
			System.exit(-1);
		}

		ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		boolean ownThread = false;
		mdfs.mount( mount, ownThread );
	}

	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	 static final String STORELOCATIONDEFAULT = "./test-store";
}

// eof
