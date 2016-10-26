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
package edu.uw.apl.tupelo.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import edu.uw.apl.commons.devicefiles.DeviceFile;

/**
 * @author Stuart Maclean
 *
 * Use DeviceFile to extract low-level data about what we call in
 * Tupelo a 'PhysicalDisk'.
 *
 * The DeviceFile class is a split Java/C beast. Currently, the C
 * parts of DeviceFile only built for Linux (MacOS, Windows to do).
 * That means that any use of this PhysicalDisk on
 * those platforms which produce an UnsatisfiedLinkError.  In these
 * cases, we <em>do</em> stagger on and the PhysicalDisk instance is
 * built.  However, actual device id and size will be unknown and we
 * use local defaults.
 *
 * @see {@link https://github.com/uw-dims/device-files}
 *
 */

public class PhysicalDisk implements UnmanagedDisk {

	public PhysicalDisk( File f ) throws IOException {
		file = f;

		/*
		  If DeviceFile fails to load its C parts, either due to being
		  run on a platform for which there are no C parts, or if that
		  loading disabled explicitly, its id and size calls will
		  fail.  We notice that here, and fill in defaults.
		*/
		try {
			DeviceFile df = new DeviceFile( f );
			id = df.getID();
			size = df.size();
		} catch( UnsatisfiedLinkError ull ) {
			System.err.println( f + ": Device details unavailable." );
			id = f.getPath();
			size = 0;
		}
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream( file );
	}

	@Override
	public File getSource() {
		return file;
	}
	
	private final File file;
	private String id;
	private long size;
}

// eof
