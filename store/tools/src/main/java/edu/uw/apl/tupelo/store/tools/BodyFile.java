package edu.uw.apl.tupelo.store.tools;

import java.io.File;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.DigestInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.LogManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

import edu.uw.apl.commons.sleuthkit.image.Image;
import edu.uw.apl.commons.sleuthkit.filesys.FileSystem;
import edu.uw.apl.commons.sleuthkit.volsys.Partition;
import edu.uw.apl.commons.sleuthkit.volsys.VolumeSystem;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileBuilder;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileCodec;

/**
 * Given the identified ManagedDiskDescriptors (either one supplied on
 * cmd line, or by enumerating over a store), locate all filesystems
 * of each managed disk and produce a 'BodyFile', using
 * tsk4j/tsk. Store the resulting BodyFile as a store attribute
 * alongside the managed disk.
 
 * Note: This program makes use of fuse4j/fuse and so has an impact on
 * the host filesystem as a whole.  In principle, a fuse mount point is
 * created at program start and deleted at program end.  However, if
 * user exits early (Ctrl C), we may have a lasting mount point.  To
 * delete this, do
 *
 * $ fusermount -u test-mount
 *
 * We do have a shutdown hook for the umount installed, but it appears
 * unreliable.
 */

public class BodyFile {

	static public class Main extends MDFSBase {
		static public void main( String[] args ) {
			Main main = new Main();
			try {
				main.readArgs( args );
				main.start();
			} catch( Exception e ) {
				System.err.println( e );
				if( debug )
					e.printStackTrace();
				System.exit(-1);
			} finally {
				LogManager.shutdown();
			}
			
		}
		
		@Override
		protected void process( File mdfsPath, ManagedDiskDescriptor mdd )
			throws Exception {
			
			/*
			  Delegate to BodyFile class, which has the core logic.
			  This way, we can have many entries points into that core logic,
			  e.g. http/server calls too.
			*/
			
			BodyFile.process( mdfsPath, mdd, store, verbose || debug );
		}
	}
	
	static public void process( File mdfsPath, ManagedDiskDescriptor mdd,
								Store store, boolean printResult )
		throws Exception {
		Log log = LogFactory.getLog( BodyFile.class );
		
		Image i = new Image( mdfsPath );
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
