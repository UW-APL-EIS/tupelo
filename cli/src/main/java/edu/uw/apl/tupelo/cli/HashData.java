/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;

import edu.uw.apl.commons.tsk4j.image.Image;
import edu.uw.apl.commons.tsk4j.filesys.Attribute;
import edu.uw.apl.commons.tsk4j.filesys.Meta;
import edu.uw.apl.commons.tsk4j.filesys.FileSystem;
import edu.uw.apl.commons.tsk4j.filesys.DirectoryWalk;
import edu.uw.apl.commons.tsk4j.filesys.Walk;
import edu.uw.apl.commons.tsk4j.filesys.WalkFile;
import edu.uw.apl.commons.tsk4j.volsys.Partition;
import edu.uw.apl.commons.tsk4j.volsys.VolumeSystem;

/**
 * Simple Tupelo Utility: Hash some previously added ManagedDisk,
 * using tsk4j/Sleuthkit routines and a FUSE filesystem to access the
 * managed data.  Store the resultant 'Hash Info' as a Store
 * attribute.
 *
 * Note: This program makes use of fuse4j/fuse and so has an impact on
 * the filesystem as a whole.  In principle, a fuse mount point is
 * created at program start and deleted at program end.  However, if
 * user exits early (Ctrl C), we may have a lasting mount point.  To
 * delete this, do
 *
 * $ fusermount -u test-mount
 *
 * We do have a shutdown hook for the umount installed, but it appears
 * unreliable.
 *
 */

public class HashData extends CliBase {

	static public void main( String[] args ) {
		HashData main = new HashData();
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

	public HashData() {
	}

	public void readArgs( String[] args ) {
		Options os = commonOptions();
		os.addOption( "v", false, "Verbose" );

		String usage = commonUsage() + "[-v] diskID sessionID";
		final String HEADER = "";
		final String FOOTER = "";
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		commonParse( os, cl, usage, HEADER, FOOTER );

		verbose = cl.hasOption( "v" );
		args = cl.getArgs();
		if( args.length < 2 ) {
			printUsage( os, usage, HEADER, FOOTER );
			System.exit(1);
		}
		diskID = args[0];
		sessionID = args[1];
	}
	
	public void start() throws Exception {

		Store store = Utils.buildStore( storeLocation );
		if( debug )
			System.out.println( "Store type: " + store );

		Collection<ManagedDiskDescriptor> stored = store.enumerate();
		System.out.println( "Stored: " + stored );

		ManagedDiskDescriptor mdd = Utils.locateDescriptor( store, diskID,
															sessionID );
		if( mdd == null ) {
			System.err.println( "Not stored: " + diskID + "," + sessionID );
			System.exit(1);
		}
			
		final ManagedDiskFileSystem mdfs = new ManagedDiskFileSystem( store );
		
		final File mountPoint = new File( "test-mount" );
		mountPoint.mkdirs();
		mountPoint.deleteOnExit();
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
		System.out.println( "Located Managed Data: " + f );
		Image i = new Image( f );

		System.out.println( "Trying volume system on " + f );
		try {
			boolean b = walkVolumeSystem( i );
			if( !b ) {
				System.out.println( "Trying file system on " + f );
				walkFileSystem( i );
			}
		} finally {
			// MUST release i else leaves mdfs non-unmountable
			i.close();
		}
	}
	

	/**
	 * @return true if a volume system found (and thus traversed), false
	 * otherwise.  False result lets us try the image as a standalone
	 * filesystem
	 */
	private boolean walkVolumeSystem( Image i ) throws Exception {

		VolumeSystem vs = null;
		try {
			vs = new VolumeSystem( i );
		} catch( Exception iae ) {
			return false;
		}
		
		List<Partition> ps = vs.getPartitions();
		try {
			for( Partition p : ps ) {
				if( !p.isAllocated() )
					continue;
				System.out.println( "At sector " + p.start() +
									", located " + p.description() );
				Map<String,byte[]> fileHashes = new HashMap<String,byte[]>();
				FileSystem fs = new FileSystem( i, p.start() );
				walk( fs, fileHashes );
				fs.close();
				System.out.println( " FileHashes : " + fileHashes.size() );
				record( fileHashes, p.start(), p.length() );
			}
		} finally {
			// MUST release vs else leaves mdfs non-unmountable
			vs.close();
		}
		return true;
	}

	private void walkFileSystem( Image i ) throws Exception {
		Map<String,byte[]> fileHashes = new HashMap<String,byte[]>();
		FileSystem fs = new FileSystem( i );
		try {
			walk( fs, fileHashes );
			System.out.println( "FileHashes: " + fileHashes.size() );
			// signify a standalone file system via a 0,0 sector interval
			record( fileHashes, 0, 0 );
		} finally {
			fs.close();
		}
	}
	
	private void walk( FileSystem fs,
					   final Map<String,byte[]> fileHashes )
		throws Exception {
		DirectoryWalk.Callback cb = new DirectoryWalk.Callback() {
				public int apply( WalkFile f, String path ) {
					try {
						process( f, path, fileHashes );
						return Walk.WALK_CONT;
					} catch( Exception e ) {
						System.err.println( e );
						return Walk.WALK_ERROR;
					}
				}
			};
		int flags = DirectoryWalk.FLAG_NONE;
		// LOOK: visit deleted files too ??
		flags |= DirectoryWalk.FLAG_ALLOC;
		flags |= DirectoryWalk.FLAG_RECURSE;
		flags |= DirectoryWalk.FLAG_NOORPHAN;
		fs.dirWalk( fs.rootINum(), flags, cb );
		fs.close();
	}

	private void process( WalkFile f, String path,
						  Map<String,byte[]> fileHashes )
		throws IOException {

		String name = f.getName();
		if( name == null )
			return;
		if(	"..".equals( name ) || ".".equals( name ) ) {
			return;
		}
		Meta m = f.meta();
		if( m == null )
			return;
		// LOOK: hash directories too ??
		if( m.type() != Meta.TYPE_REG )
			return;
		Attribute defa = f.getAttribute();
		// Seen some weirdness where an allocated file has no attribute(s) ??
		if( defa == null )
			return;

		if( debug )
			System.out.println( "'" + path + "' '" + name + "'" );

		String wholeName = path + name;
		byte[] digest = digest( defa );
		fileHashes.put( wholeName, digest );
	}
	
	private byte[] digest( Attribute a ) throws IOException {
		MD5.reset();
		InputStream is = a.getInputStream();
		DigestInputStream dis = new DigestInputStream( is, MD5 );
		while( true ) {
			int nin = dis.read( DIGESTBUFFER );
			if( nin < 0 )
				break;
		}
		return MD5.digest();
	}
	
	private void record( Map<String,byte[]> fileHashes,
						 long start, long length )
		throws Exception {

		List<String> sorted = new ArrayList<String>( fileHashes.keySet() );
		Collections.sort( sorted );
		String outName = diskID + "-" + sessionID + "-" +
			start + "-" + length + ".md5";
		System.out.println( "Writing: " + outName );
		
		FileWriter fw = new FileWriter( outName );
		BufferedWriter bw = new BufferedWriter( fw, 1024 * 1024 );
		PrintWriter pw = new PrintWriter( bw );
		for( String fName : sorted ) {
			byte[] hash = fileHashes.get( fName );
			String s = new String( Hex.encodeHex( hash ) );
			pw.println( s + " " + fName );
		}
		pw.close();
	}
	

	String diskID, sessionID;
	static boolean verbose;

	static byte[] DIGESTBUFFER = new byte[ 1024*1024 ];
	static MessageDigest MD5 = null;
	static {
		try {
			MD5 = MessageDigest.getInstance( "md5" );
		} catch( NoSuchAlgorithmException never ) {
		}
	}
}

// eof
