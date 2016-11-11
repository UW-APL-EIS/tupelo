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
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

/**
 * @author Stuart Maclean
 *
 * The Status command takes a list of device names Di (possibly empty)
 * from the local configuration.  It then checks to all stores S in
 * the location configuration to see if Di has ever been pushed any
 * Sj.
 *
 * This answers the question 'Has this disk ever been acquired by
 * Tupelo?'
 *
 *
 */

public class StatusCmd extends Command {
	StatusCmd() {
		super( "status" );
	}

	@Override
	public void invoke( Config config, boolean verbose,
						CommandLine cl )
		throws Exception {

		List<Config.Device> cds = config.devices();
		List<Config.Store>  css = config.stores();

		/*
		  Need a simple 'Pair' class that zips together a Store and a
		  Config.Store, since both needed to report a 'device was pushed
		  to store hit'
		*/
		class StorePair {
			StorePair( Store s, Config.Store c ) {
				store = s;
				config = c;
			}
			final Store store;
			final Config.Store config;
		}
		
		List<StorePair> sps = new ArrayList( css.size() );
		for( Config.Store cs : css ) {
			Store s = null;
			try {
				s = createStore( cs );
				StorePair sp = new StorePair( s, cs );
				sps.add( sp );
				if( verbose )
					System.out.println( cs + " -> " + s );
			} catch( IOException ioe ) {
				System.err.println( ioe.getMessage() );
				continue;
			}
		}

		String header = String.format( HITREPORTFORMAT,
									   "Device", "ID", "Store",
									   "Path", "Session" );
		System.out.println( header );
		System.out.println();

		for( Config.Device cd : cds ) {
			for( StorePair sp : sps ) {
				Store s = sp.store;
				Collection<ManagedDiskDescriptor> mdds = s.enumerate();
				for( ManagedDiskDescriptor mdd : mdds ) {
					if( cd.getID().equals( mdd.getDiskID() ) ) {
						Config.Store cs = sp.config;
						reportHit( cd, cs, mdd );
					}
				}
			}
		}
	}

	private void reportHit( Config.Device cd, Config.Store cs,
							ManagedDiskDescriptor mdd ) {
		String hit = String.format( HITREPORTFORMAT,
									cd.getName(), cd.getID(),
									cs.getName(), cs.getUrl(),
									mdd.getSession() );
		System.out.println( hit );
	}
						   
	static final String HITREPORTFORMAT = "%8s %24s %8s %16s %16s";
}

// eof
