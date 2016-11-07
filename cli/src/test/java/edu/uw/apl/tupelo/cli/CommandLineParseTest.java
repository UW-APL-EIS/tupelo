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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.cli.*;

/**
 * @author Stuart Maclean.
 *
 * Simple unit tests of the commons.cli classes.  Reason for this is
 * we are interested in tup cmd line options like git, where can have
 * globalOptions AND subCommand option too, like this:
 *
 * tup -c configFile -v device add -X ...
 *
 * Global options are those found before the subCommand, 'device' in this case.
 */
public class CommandLineParseTest {

	@Test
	public void globalOptions1() {
		Options os = new Options();
		os.addOption( "p", "longP", true, "P desc" );
		CommandLineParser clp = new DefaultParser();
		String[] args = { "-p", "P" };
		try {
			CommandLine cl = clp.parse( os, args );
			args = cl.getArgs();
			assertEquals( args.length, 0 );
		} catch( ParseException pe ) {
			fail();
		}
	}

	@Test
	public void globalOptions2() {
		Options os = new Options();
		os.addOption( "p", "longP", true, "P desc" );
		os.addOption( "q", "longQ", false, "Q desc" );
		CommandLineParser clp = new DefaultParser();
		String[] args = { "-p", "P" };
		try {
			CommandLine cl = clp.parse( os, args );
			args = cl.getArgs();
			assertEquals( args.length, 0 );
		} catch( ParseException pe ) {
			fail();
		}
	}

	@Test
	public void globalOptions3() {
		Options os = new Options();
		os.addOption( "p", "longP", true, "P desc" );
		os.addOption( "q", "longQ", false, "Q desc" );
		CommandLineParser clp = new DefaultParser();
		String[] args = { "-p", "P", "cmd", "-a" };
		try {
			boolean stopAtNonOption = true;
			CommandLine cl = clp.parse( os, args, stopAtNonOption );
			args = cl.getArgs();
			assertTrue( cl.hasOption( "p" ) );
			assertFalse( cl.hasOption( "q" ) );
			assertEquals( args.length, 2 );
		} catch( ParseException pe ) {
			fail( "" + pe );
		}
	}

	@Test
	public void cmdLine1() {
		Options os = new Options();
		os.addOption( "c", "longC", true, "config file" );
		os.addOption( "v", "", false, "verbose" );
		CommandLineParser clp = new DefaultParser();
		String[] args = { "-c", "FILE", "-v", "sub", "subArg", "-X" };
		try {
			boolean stopAtNonOption = true;
			CommandLine cl = clp.parse( os, args, stopAtNonOption );
			args = cl.getArgs();
			assertTrue( cl.hasOption( "c" ) );
			assertTrue( cl.hasOption( "v" ) );
			assertEquals( args.length, 3 );
			assertEquals( args[0], "sub" );
		} catch( ParseException pe ) {
			fail( "" + pe );
		}
	}

	@Test
	public void cmdLine2() {
		Options os = new Options();
		os.addOption( "c", "longC", true, "config file" );
		os.addOption( "v", "", false, "verbose" );
		CommandLineParser clp = new DefaultParser();
		String[] args = { "-c", "FILE", "-v", "sub", "subArg", "-X" };
		try {
			boolean stopAtNonOption = true;
			CommandLine cl = clp.parse( os, args, stopAtNonOption );
			args = cl.getArgs();
			assertTrue( cl.hasOption( "c" ) );
			assertTrue( cl.hasOption( "v" ) );
			assertEquals( args.length, 3 );
			assertEquals( args[0], "sub" );

			String cmd = args[0];
			String[] subArgs = new String[args.length-1];
			System.arraycopy( args, 1, subArgs, 0, subArgs.length );

			os = new Options();
			os.addOption( "X", "", false, "X" );
			stopAtNonOption = false;
			cl = clp.parse( os, subArgs, stopAtNonOption );
			args = cl.getArgs();
			assertTrue( args.length == 1 );
		} catch( ParseException pe ) {
			fail( "" + pe );
		}
	}
}