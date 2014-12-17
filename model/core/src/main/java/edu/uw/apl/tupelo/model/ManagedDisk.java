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
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

	abstract public void setParentDigest( ManagedDiskDigest grainHashes );

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
	

	static public class Header {

		/**
		 * @param capacity - disk size, in sectors (as per VMDKs)
		 * @param grainSize - grain size, in sectors (as per VMDKs)
		 */
		public Header( String diskID, Session s,
					   DiskTypes type, UUID uuidParent, long capacity,
					   long grainSize ) {
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
			dataOffset = di.readLong();
			padding = di.readInt();
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
			4 + 8 + 8 + 8 +		// numGTEsPerGT, offset x 3
			+ 4 +				// padding
			4;					// compressAlgorithm

		// The number of bytes allocated on disk for a Header
		static public final int SIZEOF = 512;

	}

	Header header;

	// Only one of these is valid, never both
	protected UnmanagedDisk unmanagedData;// for creating/writing a ManagedDisk
	protected File managedData;		// for loading a ManagedDisk
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

	// As used by StreamoptimizedDisk and Store impls for digesting a managed disk
	static public final String DIGESTALGORITHM = "md5";
	
}

// eof
