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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import edu.uw.apl.vmvols.model.VirtualMachine;
import edu.uw.apl.vmvols.model.virtualbox.VBoxVM;
import edu.uw.apl.vmvols.model.virtualbox.VDIDisk;
import edu.uw.apl.vmvols.model.vmware.VMDKDisk;
import edu.uw.apl.vmvols.model.vmware.VMwareVM;

/**
 * A simple bridge class, bridging from Tupelo model objects
 * (UnmanagedDisk) to the vmvols artifact (outside of Tupelo) which
 * enables VirtualBox (vdi) and VMWare (vmdk) disk content to be read
 * on the host.
 *
 * VMs should be powered OFF when reading them on the host,
 * e.g. during Tupelo operations.
 */
public class VirtualDisk implements UnmanagedDisk {

	static public boolean likelyVirtualDisk( File f ) {
		if( f.getName().endsWith( VDIDisk.FILESUFFIX ) )
			return true;
		if( f.getName().endsWith( VMDKDisk.FILESUFFIX ) )
			return true;
		if( VBoxVM.isVBoxVM( f ) )
			return true;
		if( VMwareVM.isVMwareVM( f ) )
			return true;
		return false;
	}
	
	public VirtualDisk( File f ) throws IOException {
		vm = VirtualMachine.create( f );
		if( vm == null )
			throw new IllegalArgumentException( "Unknown vd file: " + f );
		List<edu.uw.apl.vmvols.model.VirtualDisk> disks =
			vm.getActiveDisks();
		if( disks.size() > 1 ) {
			throw new IllegalArgumentException
				( "VM created from " + f + " has " + disks.size() +
						  " disks, specify one." );
		}
		delegate = disks.get(0);
		source = delegate.getPath();
	}

	@Override
	public String getID() {
		return delegate.getID();
	}

	@Override
	public long size() {
		return delegate.size();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return delegate.getInputStream();
	}

	@Override
	public File getSource() {
		return source;
	}

	public VirtualMachine getVM() {
		return vm;
	}
	
	/*
	  Pah, tried to avoid the delegate leaking out, but a caller
	  using a VirtualMachineFileSystem needs the delegate to look
	  up its path in the vmfs
	*/
	public edu.uw.apl.vmvols.model.VirtualDisk getDelegate() {
		return delegate;
	}

	private VirtualMachine vm;
	private /*final*/ edu.uw.apl.vmvols.model.VirtualDisk delegate;
	private File source;
}

// eof
