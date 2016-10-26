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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

/**
 * A container for state common to all command line-driven tools in
 * this module
 */

class Base {
	
	protected Base() {
		storeLocation = STORELOCATIONDEFAULT;
		log = Logger.getLogger( getClass() );
	}

	protected void readArgs( String[] args ) {
		Options os = commonOptions();

		String usage = commonUsage() + " (diskID sessionID)?";
		final String HEADER = "";
		final String FOOTER = "";
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		commonParse( os, cl, usage, HEADER, FOOTER );
		if( all )
			return;
		args = cl.getArgs();
		if( args.length < 2 ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
	}

	static protected void printUsage( Options os, String usage,
									  String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	protected Options commonOptions() {
		Options os = new Options();
		os.addOption( "a", false,
					  "Hash all managed disks (those done not re-computed)" );
		os.addOption( "d", false, "Debug" );
		os.addOption( "h", false, "Help" );
		os.addOption( "s", true,
					  "Store directory. Defaults to " + STORELOCATIONDEFAULT );
		os.addOption( "v", false, "Verbose" );
		return os;
	}
	
	protected String commonUsage() {
		return getClass().getName() + " [-a] [-d] [-h] [-s storeLocation] [-v]";
	}
	
	protected void commonParse( Options os, CommandLine cl, String usage,
								String header, String footer ) {
		boolean help = cl.hasOption( "h" );
		if( help ) {
			printUsage( os, usage, header, footer );
			System.exit(1);
		}
		all = cl.hasOption( "a" );
		debug = cl.hasOption( "d" );
		verbose = cl.hasOption( "v" );
		if( debug )
			log.setLevel( Level.DEBUG );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
	}

	
	protected String storeLocation;
	protected FilesystemStore store;
	protected boolean all;
	protected String diskID, sessionID;
	protected Logger log;

	static protected final String STORELOCATIONDEFAULT = "./test-store";
	
	static protected boolean debug, verbose;
}

// eof
