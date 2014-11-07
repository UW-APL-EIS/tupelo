package edu.uw.apl.tupelo.model;

import java.io.File;

public class PhysicalDiskTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testNativeLoad() throws Exception {
		File f = new File( "/dev/sda" );
		if( !f.canRead() )
			return;
		PhysicalDisk pd = new PhysicalDisk( f );
	}

	public void testNativeSize1() throws Exception {
		File f = new File( "/dev/sda" );
		if( !f.canRead() )
			return;
		PhysicalDisk pd = new PhysicalDisk( f );
		long sz = pd.size();
		System.out.println( f + " -> " + sz );
	}

	public void testNativeSize2() throws Exception {
		File f = new File( "/dev/sdb" );
		if( !f.canRead() )
			return;
		PhysicalDisk pd = new PhysicalDisk( f );
		long sz = pd.size();
		System.out.println( f + " -> " + sz );
	}


	/**
	 * On rejewski (Ubuntu Linux) the various ATA disk id infos are
	 * also available at /dev/disk/by-id/.  Your system may show
	 * same...
	 */
	public void testNativeSerialNum1() throws Exception {
		File f = new File( "/dev/sda" );
		if( !f.canRead() )
			return;
		PhysicalDisk pd = new PhysicalDisk( f );
		String id = pd.getID();
		System.out.println( f + " -> " + id );
	}

	public void testNativeSerialNum2() throws Exception {
		File f = new File( "/dev/sdb" );
		if( !f.canRead() )
			return;
		PhysicalDisk pd = new PhysicalDisk( f );
		String id = pd.getID();
		System.out.println( f + " -> " + id );
	}
}

// eof
