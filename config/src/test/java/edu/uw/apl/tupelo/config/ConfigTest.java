package edu.uw.apl.tupelo.config;

import java.io.File;

public class ConfigTest extends junit.framework.TestCase {

	public void testEmpty() throws Exception {
		Config c = new Config();
		c.store( new File( "config.empty" ) );
	}

	public void testAddStore() throws Exception {
		Config c = new Config();
		c.addStore( "S1", "URL" );
		c.store( new File( "config.store" ) );
	}

	public void testAddDevice() throws Exception {
		Config c = new Config();
		c.addDevice( "A", "/dev/sda" );
		c.store( new File( "config.device" ) );
	}

	public void testCat() throws Exception {
		Config c = new Config();
		c.addDevice( "A", "/dev/sda" );
		c.addStore( "S1", "URL" );
		c.addStore( "S2", "URL" );
		c.addDevice( "B", "/dev/sdb" );
		c.store( System.out );
	}

	public void testLoad1() throws Exception {
		Config c = new Config();
		c.load( new File( "config.store" ) );
		c.store( System.out );
	}

	public void testRoundtrip1() throws Exception {
		Config c = new Config();
		c.addDevice( "RTA", "/dev/sda" );
		c.addStore( "RTS1", "URL" );
		c.addStore( "RTS2", "URL" );
		c.addDevice( "RTB", "/dev/sdb" );
		File f = new File( "rt" );
		c.store( f );
		c = null;
		Config c2 = new Config();
		c2.load( f );
		c2.store( System.out );
	}
}

// eof
