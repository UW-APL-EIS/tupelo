package edu.uw.apl.tupelo.model;

import java.io.File;

public class DeviceFileLoadFailTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testSDA() throws Exception {
		File f = new File( "/dev/sda" );
		if( !f.canRead() )
			return;
		System.setProperty( "edu.uw.apl.commons.devicefiles.device-files.disabled",
							"true" );
		PhysicalDisk pd = new PhysicalDisk( f );
	}
}

// eof
