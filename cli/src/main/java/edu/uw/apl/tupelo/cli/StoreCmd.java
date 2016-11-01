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

import edu.uw.apl.tupelo.config.Config;
	
public class StoreCmd extends Command {

	StoreCmd() {
		super( "store" );
		/*
		addSub( "list", new Lambda() {
				public void invoke( CommandLine cl, String[] args, Config c )
					throws Exception {
					list( c );
				}
			} );
		addSub( "add", new Lambda() {
				public void invoke( CommandLine cl, String[] args,
									Config c ) throws Exception {
					add( cl, args, c );
				}
			} );
		addSub( "remove", new Lambda() {
				public void invoke( CommandLine cl, String[] args,
									Config c ) throws Exception {
					remove( cl, args, c );
				}
			} );
		*/
	}

	@Override
	public void invoke( Config config, boolean verbose,
						String[] args, CommandLine cl )
		throws Exception {
	}
	
	private void list( Config c ) {
		for( Config.Store s : c.stores() ) {
			System.out.println( s.getName() );
			System.out.println( " path = " + s.getUrl() );
		}
	}

	private void add( CommandLine cl, String[] args, Config c )
		throws Exception {
		if( args.length >= 2 ) {
			String name = args[0];
			String url = args[1];
			c.addStore( name, url );
			c.store();
		}
	}
	
	private void remove( CommandLine cl, String[] args, Config c )
		throws Exception {

		if( args.length < 1 ) {
			HelpCmd.INSTANCE.commandHelp( this );
			return;
		}
		String name = args[0];
		c.removeStore( name );
		c.store();
	}

}

// eof
