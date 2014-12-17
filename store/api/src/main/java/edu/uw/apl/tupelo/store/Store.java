package edu.uw.apl.tupelo.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.model.Session;

public interface Store {

	public UUID getUUID() throws IOException;
	
	public long getUsableSpace() throws IOException;
	
	public Session newSession() throws IOException;

	public void put( ManagedDisk md ) throws IOException;

	public void put( ManagedDisk md, ProgressMonitor.Callback cb,
					 int progressUpdateIntervalSecs ) throws IOException;

	/**
	 * @return size, in bytes, of the managed disk described by the
	 * supplied descriptor. Return -1 if the descriptor does not
	 * identify a managed disk held in this store.
	 */
	public long size( ManagedDiskDescriptor mdd ) throws IOException;

	public UUID uuid( ManagedDiskDescriptor mdd ) throws IOException;

	public ManagedDiskDigest digest( ManagedDiskDescriptor mdd )
		throws IOException;
	
	public Collection<String> listAttributes( ManagedDiskDescriptor mdd )
		throws IOException;

	public void setAttribute( ManagedDiskDescriptor mdd,
							  String key, byte[] value ) throws IOException;


	public byte[] getAttribute( ManagedDiskDescriptor mdd, String key )
		throws IOException;
	
	// for the benefit of the fuse-based ManagedDiskFileSystem
	public ManagedDisk locate( ManagedDiskDescriptor mdd );
	
	public Collection<ManagedDiskDescriptor> enumerate() throws IOException;

	/**
	 * Use with caution, could LOSE data
	 */
	//	public void clear();
}

// eof
