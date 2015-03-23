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
		if( VBoxVM.isVBox( f ) )
			return true;
		if( VMwareVM.isVMware( f ) )
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
	  up its pth in the vmfs
	*/
	public edu.uw.apl.vmvols.model.VirtualDisk getDelegate() {
		return delegate;
	}

	private VirtualMachine vm;
	private /*final*/ edu.uw.apl.vmvols.model.VirtualDisk delegate;
	private File source;
}

// eof
