package edu.uw.apl.tupelo.store;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public interface Store {

	public UUID getUUID() throws IOException;
	
	public long getUsableSpace() throws IOException;
	
	public Session newSession() throws IOException;

	public void put( ManagedDisk md ) throws IOException;

	public Collection<ManagedDiskDescriptor> enumerate() throws IOException;

	/**
	 * Use with caution, could LOSE data
	 */
	//	public void clear();
}

// eof
