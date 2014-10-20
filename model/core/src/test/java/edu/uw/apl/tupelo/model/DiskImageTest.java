package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DiskImageTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testSize64k() throws IOException {
		File f = new File( "src/test/resources/64k" );
		if( !f.exists() )
			return;
		DiskImage di = new DiskImage( f );
		assertEquals( di.size(), 64 * 1024 );
		assertEquals( di.getID(), f.getName() );
	}
	
}

// eof
