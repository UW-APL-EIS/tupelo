package edu.uw.apl.tupelo.model;

import java.util.regex.Pattern;

/**
   A ManagedDiskDescriptor (bad name but what is better?) is essentially a pair:

   1 volume id - a string

   2 session id

   so captures 'what' and 'when'

   
   When embedded in a filesystem directory within e.g. a
   FilesystemStore, an MDM becomes

   /path/to/store/.../VID/SESSIONID/...

   and subdirs like 'data' and 'attrs' then follow.

   Similarly, when embedded in a http url or amqp message, an MDM becomes

   ../VID/SESSIONID/...

   The ordering of the two members in the formatted descriptors
   follows that of increasing entropy: vol id very stable (should
   never change!), session id more dynamic (since disk contents are a
   function of time)
*/

public class ManagedDiskDescriptor implements java.io.Serializable {

	public ManagedDiskDescriptor( String diskID, Session s ) {
		this.diskID = diskID;
		this.session = s;
	}

	public boolean equals( Object o ) {
		if( this == o )
			return true;
		if( !( o instanceof ManagedDiskDescriptor ) )
			return false;
		ManagedDiskDescriptor that = (ManagedDiskDescriptor)o;
		return this.diskID.equals( that.diskID ) &&
			this.session.equals( that.session );
	}
	
	// maintain the general contract for equal Objects, though Map usage unlikely
	public int hashCode() {
		return diskID.hashCode() + session.hashCode();
	}

	public String getDiskID() {
		return diskID;
	}
	
	public Session getSession() {
		return session;
	}

	// useful only for debug, NEVER as a real filesystem path, name or url part..
	public String toString() {
		return "(" + getDiskID() + "," +
			getSession().toString() + ")" ;
	}
	
	private final String diskID;
	private final Session session;

	static public final Pattern DISKIDREGEX = Pattern.compile
		( "[A-Za-z0-9_:\\-\\. ]+" );

}

// eof