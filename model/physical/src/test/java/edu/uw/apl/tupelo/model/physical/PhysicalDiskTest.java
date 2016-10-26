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

	/*
	  On rejewski, which has 2 hard drives (sda,sdb), an attached
	  thumb drive (memory stick) appears as /dev/sdc
	  
	  Resultant ID :

	  /dev/sdc -> Flash-Drive AU_USB20-

	  This appears in /dev/disk/by-id as

	  usb-Flash_Drive_AU_USB20_M8N31LC8-0:0

	  So somewhere my local C/JNI code is losing information.  May need
	  another avenue into the kernel, likely a usb device would NOT use
	  a scsi system call.
	  
	*/
	public void testNativeSerialNum3() throws Exception {
		File f = new File( "/dev/sdc" );
		if( !f.canRead() )
			return;
		PhysicalDisk pd = new PhysicalDisk( f );

		/*
		String vid = pd.vendorID();
		System.out.println( f + " vendor -> " + vid );

		String pid = pd.productID();
		System.out.println( f + " product -> " + pid );

		String sn = pd.serialNumber();
		System.out.println( f + " serialnum -> " + sn );
		*/
	}
}

// eof
