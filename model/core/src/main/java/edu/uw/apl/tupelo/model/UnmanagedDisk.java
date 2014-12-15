package edu.uw.apl.tupelo.model;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * An interface for the core 'user data' we are trying to manage.  We
 * expect three variants for an UnmanagedDisk:
 *
 * DiskImage - a regular file on disk that represents a whole disk image
 * This implementation is in this core package/module.
 *
 * PhysicalDisk - a whole disk device file, e.g. /dev/sda.  Has its
 * own package/submodule since needs some native/JNI code to query the
 * device for id and length/size, and we do NOT want to have JNI dependency
 * on our core model objects.
 *
 * VirtualDisk - a disk from a virtual machine, such as that managed
 * by VirtualBox, VMWare.  Has its own package/submodule, and uses the
 * 'vmvols' artifact
 */
 
public interface UnmanagedDisk {

	public String getID();
	
	public long size();

	public InputStream getInputStream() throws IOException;

	// for debug, let use see the source file
	public File getSource();
}

// eof
