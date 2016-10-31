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
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;

import edu.uw.apl.commons.tsk4j.image.Image;
import edu.uw.apl.commons.tsk4j.volsys.VolumeSystem;
import edu.uw.apl.commons.tsk4j.digests.VolumeSystemHash;
import edu.uw.apl.commons.tsk4j.digests.VolumeSystemHashCodec;

/**
 * Simple Tupelo Utility: Walk some previously added ManagedDisk,
 * using tsk4j/Sleuthkit routines and a FUSE filesystem to access the
 * managed data.  The 'walk' visits every UNALLOCATED partition in the
 * VolumeSystem and produces a sha1 hash of its content.  We visit
 * only unallocated parts of the disk since we need much finer grained
 * inspection of filesystems.  Different results for a hash at time
 * T1, T2 over a partition containing a filesystem would not tell you
 * WHICH file(s) changed, so see e.g. {@link HashFS} for that.
 *
 * If the -a option is supplied, we store the result (a multi-line
 * formatted string) back to the Tupelo store as an attribute of the
 * managed disk.
 *
 * Note: This program makes use of fuse4j/fuse and so has an impact on
 * the host filesystem as a whole.  In principle, a fuse mount point
 * is created at program start and deleted at program end.  However,
 * if user exits early (Ctrl C), we may have a lasting mount point.
 * To delete this, do
 *
 * $ fusermount -u test-mount
 *
 * We do have a shutdown hook for the umount installed, but it appears
 * unreliable.
 *
 */

public class HashVS {

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
			throws IOException {
			
			HashVS.process( mdfsPath, mdd, store );
		}
	}
	
	/*
	  In addition to being callable locally by this package, the core
	  logic of this class is available to outside callers, notably the
	  http/server, which has no 'cmd line'.  Note the local use of
	  commons-logging, re-iterating the point that this method has
	  many caller scenarios.
	*/
	static public void process( File mdfsPath, ManagedDiskDescriptor mdd,
								Store store ) throws IOException {
		
		Log log = LogFactory.getLog( HashVS.class );
		log.info( "Hashing " + mdd );
		String key = "hashvs";
		byte[] value = store.getAttribute( mdd, key );
		if( value != null )
			return;
		
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
				VolumeSystemHash vsh = VolumeSystemHash.create( vs );
				StringWriter sw = new StringWriter();
				VolumeSystemHashCodec.writeTo( vsh, sw );
				String s = sw.toString();
				value = s.getBytes();
				store.setAttribute( mdd, key, value );
			} finally {
				vs.close();
			}
		} finally {
			i.close();
		}
	}
}

// eof
