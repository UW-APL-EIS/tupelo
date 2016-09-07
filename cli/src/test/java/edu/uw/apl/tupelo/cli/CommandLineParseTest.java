package edu.uw.apl.tupelo.cli;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.commons.cli.*;

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
			CommandLine cl = clp.parse( os, args, true );
			args = cl.getArgs();
			assertEquals( args.length, 2 );
		} catch( ParseException pe ) {
			fail( "" + pe );
		}
	}
}