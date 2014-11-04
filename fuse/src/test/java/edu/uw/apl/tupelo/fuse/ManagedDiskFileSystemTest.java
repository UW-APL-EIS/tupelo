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

	/**
	 * Test that we can mount and unmount a fuse filesystem representing
	 * the contents of a Tupelo store.
	 */
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

	/**
	 * Test that we can mount a fuse filesystem representing the
	 * contents of a Tupelo store, add a managed disk to that store
	 * and then locate that disk by name in the advertised filesystem,
	 * using a simple ls (LOOK: just use new File() ??). The ls should
	 * return zero.
	 */
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

	/**
	 * Test that we can mount a fuse filesystem representing the
	 * contents of a Tupelo store, and then attempt a locate of some
	 * bogus file by name in the advertised filesystem, using a simple
	 * ls (LOOK: just use new File() ??).  The ls should return
	 * non-zero.
	 */
	public void testCannotAccessBogus() throws Exception {
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

		// Now attempt to locate in the mdfs some bogus file by name...
		String cmd = "ls " + mount + "/FOOBARBAZ";
		System.out.println( cmd );
		Process p = Runtime.getRuntime().exec( cmd );
		p.waitFor();
		int sc = p.exitValue();
		assertTrue( sc != 0 );

		sc = mdfs.umount();
		assertEquals( sc, 0 );
	}
}

// eof

												