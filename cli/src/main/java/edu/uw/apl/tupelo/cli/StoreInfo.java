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

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

/**
 * Simple Tupelo Utility: Query and store for its contents
 * (ManagedDisks and Attributes) and print results to stdout.
 */

public class StoreInfo extends CliBase {

	static public void main( String[] args ) {
		StoreInfo main = new StoreInfo();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			if( debug )
				e.printStackTrace();
			System.exit(-1);
		}
		LogManager.shutdown();
	}

	public StoreInfo() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		String usage = commonUsage();
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
	}
	
	public void start() throws Exception {

		Store store = Utils.buildStore( storeLocation );
		if( debug )
			System.out.println( "Store: " + store );
		log.info( store.getClass() + " " + storeLocation );
		
		System.out.println( "Using store: " + storeLocation );
		System.out.println( "Usable Space: " + store.getUsableSpace() );
		System.out.println( "UUID: " + store.getUUID() );

		Collection<ManagedDiskDescriptor> stored = null;
		try {
			stored = store.enumerate();
		} catch( ConnectException ce ) {
			System.err.println( "Network Error. Is the remote Tupelo store up?" );
			System.exit(0);
		}
		
		System.out.println( "ManagedDisks:" );
		for( ManagedDiskDescriptor mdd : stored ) {
			report( mdd, store );
		}
	}

	private void report( ManagedDiskDescriptor mdd, Store store )
		throws IOException {
		System.out.println( mdd.getDiskID() + " , " + mdd.getSession() );
		System.out.println( " Size: " + store.size( mdd ) );
		Collection<String> attrNames = store.listAttributes( mdd );
		System.out.println( " Attributes: " + attrNames );
	}
}

// eof
