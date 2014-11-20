package edu.uw.apl.tupelo.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.model.Session;

public interface Store {

	public UUID getUUID() throws IOException;
	
	public long getUsableSpace() throws IOException;
	
	public Session newSession() throws IOException;

	public void put( ManagedDisk md ) throws IOException;

	public void put( ManagedDisk md, ProgressMonitor.Callback cb,
					 int progressUpdateIntervalSecs ) throws IOException;

	public UUID uuid( ManagedDiskDescriptor mdd ) throws IOException;

	public List<byte[]> digest( ManagedDiskDescriptor mdd )
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
