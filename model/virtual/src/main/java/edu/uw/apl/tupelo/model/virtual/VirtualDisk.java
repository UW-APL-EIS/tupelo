package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import edu.uw.apl.vmvols.model.virtualbox.VBoxVM;
import edu.uw.apl.vmvols.model.virtualbox.VDIDisk;
import edu.uw.apl.vmvols.model.vmware.VMDKDisk;

/**
 * A simple bridge class, bridging from Tupelo model objects
 * (UnmanagedDisk) to * the vmvols artifact (outside of Tupelo) which
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
		return false;
	}
	
	public VirtualDisk( File f ) throws IOException {
		if( f.isDirectory() ) {
			if( VBoxVM.isVBox( f ) ) {
				VBoxVM vm = new VBoxVM( f );
				List<edu.uw.apl.vmvols.model.VirtualDisk> disks =
					vm.getActiveDisks();
				if( disks.size() == 1 ) {
					delegate = disks.get(0);
					source = f;
				} else {
					throw new IllegalArgumentException
						( "VBox VM in " + f + " has " + disks.size() +
						  " disks, specify one." );
				}
			} else {
				throw new IllegalStateException
					( "Unable to locate a disk image: " + f );
			}
		} else {
			if( false ) {
			} else if( f.getName().endsWith( VMDKDisk.FILESUFFIX ) ) {
				VMDKDisk vmdk = VMDKDisk.readFrom( f );
				delegate = vmdk;
				source = f;
			} else if( f.getName().endsWith( VDIDisk.FILESUFFIX ) ) {
				File dir = f.getParentFile();
				if( VBoxVM.isVBox( dir ) ) {
					VBoxVM vm = new VBoxVM( dir );
					List<edu.uw.apl.vmvols.model.VirtualDisk> disks =
						vm.getActiveDisks();
					if( disks.size() == 1 ) {
						delegate = disks.get(0);
						source = f;
					} else {
						throw new IllegalArgumentException
							( "VBox VM in " + f + " has " + disks.size() +
							  " disks, specify one." );
					}
				}
			} else {
				throw new IllegalStateException
					( "Unable to locate a disk image: " + f );
			}
		}
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
	
	private /*final*/ edu.uw.apl.vmvols.model.VirtualDisk delegate;
	private File source;
}

// eof
