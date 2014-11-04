package edu.uw.apl.tupelo.fuse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import fuse.FuseMount;

import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

public class ManagedDiskFileSystemTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testMountUnmount() throws Exception {
		boolean loadManagedDisks = false;
		Store store = new FilesystemStore( new File( "test-store" ),
										   loadManagedDisks );

		File mount = new File( "test-mount" );
		mount.mkdirs();
		ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );

		boolean ownThread = true;
		mdfs.mount( mount, ownThread );

		// Wait for the mount point to become available
		Thread.sleep( 1000 * 4 );

		// and just unmount it
		int sc = mdfs.umount();
		assertEquals( sc, 0 );
	}

	public void testCanAccessPut() throws Exception {
		boolean loadManagedDisks = false;
		Store store = new FilesystemStore( new File( "test-store" ),
										   loadManagedDisks );

		File f = new File( "src/test/resources/1m" );
		if( !f.exists() )
			return;

		Session session = Session.CANNED;
		DiskImage di = new DiskImage( f );
		FlatDisk fd1 = new FlatDisk( di, session );
		store.put( fd1 );

		File mount = new File( "test-mount" );
		mount.mkdirs();
		ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		boolean ownThread = true;
		mdfs.mount( mount, ownThread );

		// Wait for the mount point to become available
		Thread.sleep( 1000 * 4 );

		// Now locate in the mdfs the data we just put...
		String cmd = "ls " + mount + "/" + f.getName() + "/" + session;
		System.out.println( cmd );
		Process p = Runtime.getRuntime().exec( cmd );
		p.waitFor();
		int sc = p.exitValue();
		assertEquals( sc, 0 );

		sc = mdfs.umount();
		assertEquals( sc, 0 );
	}
}

// eof

												