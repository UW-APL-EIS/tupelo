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
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Stuart Maclean
 *
 * A ManagedDisk is the 'unit of work' for a Tupelo store, i.e. what a
 * Tupelo store holds for 'whole disk image captured'.  A ManagedDisk
 * has a Header, at offset 0.  The StreamOptimizedDisk variant also
 * has a Header at end.  The Header(s) include copies of the
 * UnmanagedDisk diskID that identifies the unmanaged data, plus a
 * Session, and also a UUID for create and parent.  The parent UUID is
 * how we link one ManagedDisk object to a previous capture of the
 * same data.
 *
 * @see FlatDisk
 * @see StreamOptimizedDisk
 */

abstract public class ManagedDisk {

	/**
	 * Expected one and only one of these is non-null at construction
	 * time.  As a ManagedDisk transitions to a store, its managedData
	 * file may be set, by that store.
	 */
	protected ManagedDisk( UnmanagedDisk unmanagedData, File managedData ) {
		this.unmanagedData = unmanagedData;
		this.managedData = managedData;
		log = LogFactory.getLog( getClass() );
	}

		
	// checkSize now not really enforcing much since we allow padding...
	protected void checkSize( long advertisedSizeBytes ) {
		if( advertisedSizeBytes % Constants.SECTORLENGTH != 0 ) {
			throw new IllegalArgumentException
				( "Data length (" + advertisedSizeBytes +
				  ") must be a multiple of " + Constants.SECTORLENGTH );
		}
	}
	
	/**
	 * Solely for the benefit of ProgressMonitor operations, who
	 * need a handle on the InputStream of the unmanagedData associated
	 * with a ManagedDisk, to monitor the Unmanaged -> Managed data transfer.
	 */
	public UnmanagedDisk getUnmanaged() {
		return unmanagedData;
	}
	
	public boolean hasParent() {
		return !header.uuidParent.equals( Constants.NULLUUID );
	}

	/**
	 * Needed by Store implementations to attached a final managedData file
	 * to a supplied ManagedDisk object (e.g. in put()).  The ManagedDisk
	 * object likely stays in memory, so needs its association to the
	 * final store location on disk.
	 */
	public void setManagedData( File f ) {
		managedData = f;
	}

	public void setCompression( Compressions c ) {
		header.compressAlgorithm = c;
	}

	public Compressions getCompression() {
		return header.compressAlgorithm;
	}
	
	abstract public void setParentDigest( ManagedDiskDigest grainHashes );

	/**
	 * Expected that the parent-child association between ManagedDisks
	 * is handled by the UUIDs held in the ManagedDisk.Header:
	 *
	 * child.parentUUID == parent.createUUID
	 */
	abstract public void setParent( ManagedDisk md );

	abstract public void reportMetaData() throws IOException;

	abstract public void writeTo( OutputStream os ) throws IOException;

	abstract public void readFromWriteTo( InputStream is, OutputStream os )
		throws IOException;

	/**
	 * Called by store implementations (e.g. filesystem store) to
	 * verify that what got stored onto store disk really is a
	 * complete ManagedDisk entity.
	 *
	 * @throws IOException
	 * @throws IllegalStateException to indicate verification failure.
	 */
	abstract public void verify() throws IOException;

	abstract public InputStream getInputStream() throws IOException;

	abstract public SeekableInputStream getSeekableInputStream()
		throws IOException;

	static public ManagedDisk readFrom( File managedDisk ) throws IOException {
		ManagedDisk result = null;
		FileInputStream fis = new FileInputStream( managedDisk );
		Header h = new Header( fis );
		fis.close();
		switch( h.type ) {
		case FLAT:
			result = new FlatDisk( managedDisk, h );
			break;
		case STREAMOPTIMIZED:
			result = new StreamOptimizedDisk( managedDisk, h );
			break;
		default:
			throw new IllegalStateException
				( managedDisk + ": Unknown ManagedDisk type " + h.type );
		}
											
		return result;
	}

	/**
	 * @return size of the managed disk, in BYTES
	 */
	public long size() {
		return header.capacity * Constants.SECTORLENGTH;
	}

	public UUID getUUIDCreate() {
		return header.uuidCreate;
	}

	public UUID getUUIDParent() {
		return header.uuidParent;
	}

	public long grainSizeBytes() {
		return header.grainSize * Constants.SECTORLENGTH;
	}
	
	protected void setHeader( Header h ) {
		header = h;
	}

	public String paramString() {
		return "TODO";
	}
	
	public ManagedDiskDescriptor getDescriptor() {
		// LOOK: create once ?
		return new ManagedDiskDescriptor( header.diskID, header.session );
	}

	// Debug inspection of important fields in the Header
	public void report( PrintStream ps ) {
		ps.println( "Type: " + getClass() );
		ManagedDiskDescriptor mdd = getDescriptor();
		ps.println( "DiskID: " + mdd.getDiskID() );
		ps.println( "Session: " + mdd.getSession().format() );
		ps.println( "Size: " + size() );
		ps.println( "UUID.Create: " + getUUIDCreate() );
		ps.println( "UUID.Parent: " + getUUIDParent() );
		ps.println( "Compression: " + getCompression() );
		ps.println( "Param: " + paramString() );
	}
	
	static public class Header {

		/**
		 * @param capacity - disk size, in sectors (as per VMDKs)
		 * @param grainSize - grain size, in sectors (as per VMDKs)
		 */
		public Header( String diskID, Session s,
					   DiskTypes type, UUID uuidParent, long capacity,
					   long grainSize, long overhead ) {
			/*
			  Record the version of the code creating/writing the header.
			  See also Version.java
			*/
			this.version = Version.VERSION;
			this.type = type;
			this.diskID = diskID;
			this.session = s;
			this.uuidCreate = UUID.randomUUID();
			this.uuidParent = uuidParent;
			this.capacity = capacity;
			this.grainSize = grainSize;
			this.numGTEsPerGT = NUMGTESPERGT;
			this.compressAlgorithm = Compressions.NONE;
			this.overhead = overhead;
		}

		Header( InputStream is ) throws IOException {
			this( (DataInput) new DataInputStream( is ) );
		}
		
		Header( DataInput di ) throws IOException {
			int n = di.readInt();
			if( n != MAGIC ) {
				throw new IllegalStateException( "Bad Magic Number!" );
			}
			// LOOK: version checking, can we reliably read/understand this data ?
			version = di.readInt();

			// LOOK: check enum bounds...
			int typeI = di.readInt();
			type = DiskTypes.values()[typeI];

			flags = di.readInt();

			long ms8 = di.readLong();
			long ls8 = di.readLong();
			uuidCreate = new UUID( ms8, ls8 );
			ms8 = di.readLong();
			ls8 = di.readLong();
			uuidParent = new UUID( ms8, ls8 );

			byte[] didBytes = new byte[DISKIDSIZEOF];
			di.readFully( didBytes );
			int i = 0;
			while( didBytes[i] != 0 )
				i++;
			diskID = new String( didBytes, 0, i );
			byte[] sessionBytes = new byte[SESSIONSIZEOF];
			di.readFully( sessionBytes );
			i = 0;
			while( sessionBytes[i] != 0 )
				i++;
			try {
				String s = new String( sessionBytes, 0, i );
				session = Session.parse( s );
			} catch( ParseException pe ) {
				// LOOK: can we really carry on ???
				session = Session.CANNED;
			}

			capacity = di.readLong();
			grainSize = di.readLong();
			numGTEsPerGT = di.readInt();
			gdOffset = di.readLong();
			rgdOffset = di.readLong();
			overhead = di.readLong();
			dataOffset = di.readLong();
			padding = di.readInt();

			// LOOK: check enum bounds...
			int compressAlgorithmInt = di.readInt();
			compressAlgorithm = Compressions.values()[compressAlgorithmInt];
		}

		// LOOK: may not need this??
		public void writeTo( OutputStream os ) throws IOException {
			DataOutputStream dos = new DataOutputStream( os );
			writeTo( (DataOutput)dos );
		}

		// can handle RandomAccessFile since that is a DataOutput
		public void writeTo( DataOutput dop ) throws IOException {
			dop.writeInt( MAGIC );
			dop.writeInt( version );
			dop.writeInt( type.ordinal() );
			dop.writeInt( flags );

			dop.writeLong( uuidCreate.getMostSignificantBits() );
			dop.writeLong( uuidCreate.getLeastSignificantBits() );
			dop.writeLong( uuidParent.getMostSignificantBits() );
			dop.writeLong( uuidParent.getLeastSignificantBits() );

			byte[] didBytes = new byte[DISKIDSIZEOF];
			byte[] ba = diskID.getBytes();
			System.arraycopy( ba, 0, didBytes, 0, ba.length );
			dop.write( didBytes );

			byte[] sessionBytes = new byte[SESSIONSIZEOF];
			ba = session.format().getBytes();
			System.arraycopy( ba, 0, sessionBytes, 0, ba.length );
			dop.write( sessionBytes );

			dop.writeLong( capacity );
			dop.writeLong( grainSize );
			dop.writeInt( numGTEsPerGT );
			dop.writeLong( gdOffset );
			dop.writeLong( rgdOffset );
			dop.writeLong( overhead );
			dop.writeLong( dataOffset );
			dop.writeInt( padding );
			dop.writeInt( compressAlgorithm.ordinal() );

			byte[] pad = new byte[SIZEOF - FIELDSIZETOTAL];
			dop.write( pad );
		}
		
		public String paramString() {
			return "TODO";
		}
		

		String diskID;
		Session session;
		final int version;
		final DiskTypes type;
		int flags;
		final UUID uuidCreate, uuidParent;
		final long capacity, grainSize;
		int numGTEsPerGT;
		long gdOffset, rgdOffset;

		// how many sectors the header and any metadata take up
		long overhead;

		long dataOffset;
		int padding;
		Compressions compressAlgorithm;
		
		// almost CODEINE, wot no N
		static public final int MAGIC = 0xC0DE10E;

		static private final int DISKIDSIZEOF = 64;
		static private final int SESSIONSIZEOF = 64;
		
		// The number of bytes required for all the Header's fields
		static private final int FIELDSIZETOTAL =
			4 + 4 + 4 + 4 +		// magic, version, type, flags
			DISKIDSIZEOF + SESSIONSIZEOF +
			16 + 16 +			// uuid x 2
			8 + 8 +				// capacity, grainSize
			4 + 8 + 8 +			// numGTEsPerGT, gdOffset, rgdOffset
			8 +					// overhead
			8 +					// data offset
			4 +					// padding
			4;					// compressAlgorithm

		// The number of bytes allocated on disk for a Header
		static public final int SIZEOF = 512;

	}

	Header header;

	/*
	  Only one of these is valid, never both.  So one is non-null, the
	  other null. When building (pushing to a store) a manageddisk,
	  there is a link to the unmanagedDisk source.  When retrieving a
	  Manageddisk from a store, there is the underlying file on disk
	  (LOOK: latter only true for FilesystemStore.  What if
	  Manageddisks stored in a DB??)
	*/

	// for creating/writing a ManagedDisk
	protected UnmanagedDisk unmanagedData;

	// for loading a ManagedDisk
	protected File managedData;		

	protected Log log;
	
	public enum DiskTypes { ERROR, FLAT, STREAMOPTIMIZED };
	
	public enum Compressions { NONE, DEFLATE, GZIP, SNAPPY };

	// Tupelo Managed Disk == tmd
	static public final String FILESUFFIX = ".tmd";
	
	static public final FilenameFilter FILEFILTER =
		new FilenameFilter() {
			public boolean accept( File dir, String name ) {
				return name.endsWith( FILESUFFIX );
			}
		};

	// In sectors, NOT bytes
	static public long GRAINSIZE_DEFAULT = 128;

	// Number of Grain Table Entries per Grain Table (the 'length' of a GT)
	static public int NUMGTESPERGT = 512;

	/*
	  As used by StreamoptimizedDisk and Store impls for digesting
	  a managed disk.
	*/
	static public final String DIGESTALGORITHM = "md5";
	
}

// eof
