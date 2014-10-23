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

	public void testEmptyStore() throws Exception {
		Store store = new FilesystemStore( new File( "test-store" ), false );

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

		/*
		try {
			// -f == no fork, -s == single-threaded
			String[] fmArgs = { mount.getName(),
								"-f", "-s" };//, "-oallow_root" };
			FuseMount.mount( fmArgs, mdfs, null );
		} catch (Exception e) {
            e.printStackTrace();
		}
		*/
		mdfs.mount( mount, false );
	}
}

// eof

												