package edu.uw.apl.tupelo.model;

import java.io.File;

public class VBoxTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testVBox1() throws Exception {
		File f = new File( "/lv1/home/stuart/VBox/nuga2" );
		if( !f.isDirectory() )
			return;
		UnmanagedDisk ud = new VirtualDisk( f );
		System.out.println( f + " " + ud.size() );
		System.out.println( f + " " + ud.getID() );
		
	}
}

// eof

	