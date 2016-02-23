package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

import edu.uw.apl.commons.sleuthkit.image.Image;
import edu.uw.apl.commons.sleuthkit.filesys.FileSystem;
import edu.uw.apl.commons.sleuthkit.volsys.Partition;
import edu.uw.apl.commons.sleuthkit.volsys.VolumeSystem;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileBuilder;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileCodec;

public class BodyfileCmd extends Command {
	BodyfileCmd() {
		super( "bodyfile", "Traverse filesystems of a store-managed disk" );
	}
	
	@Override
	public void invoke( String[] args ) throws Exception {
		Options os = commonOptions();
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
			commonParse( cl );
		} catch( ParseException pe ) {
			//	printUsage( os, usage, HEADER, FOOTER );
			//System.exit(1);
		}
		args = cl.getArgs();
		if( args.length < 2 ) {
			System.err.println( "Need store arg + index" );
			return;
		}
		Config c = new Config();
		c.load( config );
		
		String storeName = args[0];
		Config.Store selectedStore = null;
		for( Config.Store cs : c.stores() ) {
			if( cs.getName().equals( storeName ) ) {
				selectedStore = cs;
				break;
			}
		}
		if( selectedStore == null ) {
			System.err.println( "'" + storeName + "' is not a store" );
			return;
		}
		Store store = createStore( selectedStore );	

		FilesystemStore fss = (FilesystemStore)store;

		int index = Integer.parseInt( args[1] );

		Collection<ManagedDiskDescriptor> mdds = null;
		try {
			mdds = fss.enumerate();
		} catch( ConnectException ce ) {
			System.err.println
				( "Network Error. Is the remote Tupelo store up?" );
			System.exit(0);
		}
		if( index < 1 || index > mdds.size() ) {
			System.err.println( "Index out-of-range" );
			return;
		}

		final boolean debug = true;
		final boolean printResult = false;
		
		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted,
						  ManagedDiskDescriptor.DEFAULTCOMPARATOR );

		ManagedDiskDescriptor mdd = sorted.get(index-1);

		Log log = LogFactory.getLog( BodyfileCmd.class );
		log.info( "Filesystem traverse " + mdd );

		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( fss );
		
		final File mountPoint = new File( "mdfs-mount" );
		if( !mountPoint.exists() ) {
			mountPoint.mkdirs();
			mountPoint.deleteOnExit();
		}
		if( debug )
			System.out.println( "Mounting '" + mountPoint + "'" );
		mdfs.mount( mountPoint, true );
		Runtime.getRuntime().addShutdownHook( new Thread() {
				public void run() {
					if( debug )
						System.out.println( "Unmounting '" + mountPoint + "'" );
					try {
						mdfs.umount();
					} catch( Exception e ) {
						System.err.println( e );
					}
				}
			} );
		
		// LOOK: wait for the fuse mount to finish.  Grr hate arbitrary sleeps!
		Thread.sleep( 1000 * 2 );


		File f = mdfs.pathTo( mdd );
		System.err.println( f );
		Image i = new Image( f );
		try {
			VolumeSystem vs = null;
			try {
				vs = new VolumeSystem( i );
			} catch( IllegalStateException noVolSys ) {
				log.warn( noVolSys );
				return;
			}
			try {
				List<Partition> ps = vs.getPartitions();
				for( Partition p : ps ) {
					if( !p.isAllocated() ) {
						continue;
					}
					FileSystem fs = null;
					try {
						log.info( p.start() + " " +
								  p.length() + " " +
								  p.description() );
						fs = new FileSystem( i, p.start() );
						edu.uw.apl.commons.sleuthkit.digests.BodyFile bf =
							BodyFileBuilder.create( fs );
						StringWriter sw = new StringWriter();
						BodyFileCodec.format( bf, sw );
						String value = sw.toString();
						String key = "bodyfile-" +
							p.start() + "-" + p.length();
						store.setAttribute( mdd, key, value.getBytes() );
						if( printResult )
							BodyFileCodec.format( bf, System.out );
						fs.close();
					} catch( IllegalStateException noFileSystem ) {
						continue;
					}
				}				
			} finally {
				// MUST release vs else leaves mdfs non-unmountable
				vs.close();
			}
		} finally {
			// MUST release i else leaves mdfs non-unmountable
			i.close();
		}
	}
}

// eof
