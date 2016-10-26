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

import java.util.Comparator;
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

	@Override
	public boolean equals( Object o ) {
		if( this == o )
			return true;
		if( !( o instanceof ManagedDiskDescriptor ) )
			return false;
		ManagedDiskDescriptor that = (ManagedDiskDescriptor)o;
		return this.diskID.equals( that.diskID ) &&
			this.session.equals( that.session );
	}
	
	// Must maintain the general contract for equal Objects
	@Override
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

	/*
	  For a 'sensible' sort ordering that 'looks good' when displaying
	  a list of ManagedDiskDescriptors, do lexocographic sort on WHAT
	  (the disk id) followed by time order sort on WHEN (the session)

	*/
	
	static public final Comparator<ManagedDiskDescriptor>
		DEFAULTCOMPARATOR = new Comparator<ManagedDiskDescriptor>() {
		@Override
		public int compare( ManagedDiskDescriptor o1,
							ManagedDiskDescriptor o2 ) {
			int i = o1.getDiskID().compareTo( o2.getDiskID() );
			if( i != 0 )
				return i;
			return o1.getSession().compareTo( o2.getSession() );
		}
	};

	static final long serialVersionUID = 2833866706524676569L;

}

// eof
