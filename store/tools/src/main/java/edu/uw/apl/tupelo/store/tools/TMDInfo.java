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
package edu.uw.apl.tupelo.store.tools;

import java.io.File;

import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

/**
 * @author Stuart Maclean
 *
 * Print a summary of the properties of a .tmd (Tupelo Managed Disk)
 * file supplied in args[0].  Goes straight to the file, 'bypassing'
 * any store logic or layout.  NOT a user-oriented tool in the Tupelo
 * component set, more a diagnostic tool for power users (developers!)
 *
 * Usage: TMDInfo /path/to/tmdfile
 *
 * which then prints to stdout various properties (mostly from the
 * meta-data 'header' section of any ManagedDisk) of the managed data.
 */

public class TMDInfo {

	static public void main( String[] args ) {
		TMDInfo main = new TMDInfo();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			if( debug )
				e.printStackTrace();
			System.exit(-1);
		} finally {
			LogManager.shutdown();
		}
			  
	}

	public TMDInfo() {
	}

	public void readArgs( String[] args ) {
		Options os = new Options();
		os.addOption( "d", false, "Debug" );
		String usage = TMDInfo.class.getName() + " [-d] /path/to/tmdfile";
		final String header = "Print properties of given .tmd file.";
		final String footer = "";
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			HelpFormatter hf = new HelpFormatter();
			hf.setWidth( 80 );
			hf.printHelp( usage, header, os, footer );
			System.exit(1);
		}
		debug = cl.hasOption( "d" );
		args = cl.getArgs();
		if( args.length < 1 ) {
			HelpFormatter hf = new HelpFormatter();
			hf.setWidth( 80 );
			hf.printHelp( usage, header, os, footer );
			System.exit(1);
		}
		inFileName = args[0];
	}
	
	public void start() throws Exception {
		File inFile = new File( inFileName );
		if( !inFile.isFile() ) {
			throw new IllegalStateException
				( inFile + ": No such file" );
		}
		ManagedDisk md = ManagedDisk.readFrom( inFile );
		report( md, inFile );
	}

	public void report( ManagedDisk md, File source ) throws Exception {
		System.out.println( "Source: " + source );
		md.report( System.out );
	}

	static boolean debug;
	String inFileName;
}

// eof
