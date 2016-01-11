package edu.uw.apl.tupelo.config;

import java.io.File;

/**
 * @author Stuart Maclean
 *
 * Testing the 'default' tupelo config.
 */

public class DefaultTest extends junit.framework.TestCase {

	public void testEmpty() throws Exception {
		Config c = new Config();
		c.store( Config.DEFAULT );
		assertTrue( Config.DEFAULT.length() == 0 );
	}

	public void testAddStore() throws Exception {
		Config c = new Config();
		c.addStore( "S1", "URL" );
		c.store( Config.DEFAULT );
		assertTrue( Config.DEFAULT.length() > 0 );
	}

	public void testAddRemoveStore() throws Exception {
		Config c = new Config();
		c.addStore( "S1", "URL" );
		c.removeStore( "S1" );
		c.store( Config.DEFAULT );
		assertTrue( Config.DEFAULT.length() == 0 );
	}

	public void testAddRemoveDevice() throws Exception {
		Config c = new Config();
		c.addDevice( "A", "sda" );
		c.removeDevice( "A" );
		c.store( Config.DEFAULT );
		assertTrue( Config.DEFAULT.length() == 0 );
	}
}

// eof
