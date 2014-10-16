package edu.uw.apl.tupelo.store;

import java.io.IOException;
import java.util.UUID;

import edu.uw.apl.tupelo.model.Session;

public interface Store {

	public UUID getUUID() throws IOException;
	
	public long getUsableSpace() throws IOException;
	
	public Session newSession() throws IOException;
}

// eof
