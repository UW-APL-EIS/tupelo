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
import java.util.UUID;

abstract public class ManagedDisk {

	protected ManagedDisk( File rawData, File managedData ) {
		this.rawData = rawData;
		this.managedData = managedData;
	}
	
	public boolean hasParent() {
		return !header.uuidParent.equals( Constants.NULLUUID );
	}

	/**
	 * Needed by Store implementations to attached a final managedData file
	 * to an supplied ManagedDisk object (e.g. in put())
	 */
	public void setManagedData( File f ) {
		managedData = f;
	}
	
	abstract public void setParent( ManagedDisk md );
	//	abstract public ManagedDisk getParent();

	abstract public void writeTo( File f ) throws IOException;

	abstract public InputStream getInputStream() throws IOException;

	abstract public RandomAccessRead getRandomAccessRead() throws IOException;

	static public ManagedDisk readFrom( File managedDisk ) throws IOException {
		ManagedDisk result = null;
		FileInputStream fis = new FileInputStream( managedDisk );
		Header h = new Header( fis );
		fis.close();
		switch( h.type ) {
		case FLAT:
			result = new FlatDisk( managedDisk, h );
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

	protected void setHeader( Header h ) {
		header = h;
	}

	public ManagedDiskDescriptor getDescriptor() {
		// LOOK: create once ?
		return new ManagedDiskDescriptor( header.diskID, header.session );
	}
	
	static public class Header {

		/**
		 * Example: alignUp( 700, 512 ) -> 1024
		 */
		static long alignUp( long b, int a ) {
			return (long)(Math.ceil( (double)b / a ) * a);
		}

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

			byte[] didBytes = new byte[64];
			di.readFully( didBytes );
			int i = 0;
			while( didBytes[i] != 0 )
				i++;
			diskID = new String( didBytes, 0, i );
			byte[] sessionBytes = new byte[64];
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
			compressAlgorithm = di.readInt();
		}

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

			byte[] didBytes = new byte[64];
			byte[] ba = diskID.getBytes();
			System.arraycopy( ba, 0, didBytes, 0, ba.length );
			dop.write( didBytes );

			byte[] sessionBytes = new byte[64];
			ba = session.format().getBytes();
			System.arraycopy( ba, 0, sessionBytes, 0, ba.length );
			dop.write( sessionBytes );

			dop.writeLong( capacity );
			dop.writeLong( grainSize );
			dop.writeInt( numGTEsPerGT );
			dop.writeLong( gdOffset );
			dop.writeLong( rgdOffset );
			dop.writeLong( dataOffset );
			dop.writeInt( compressAlgorithm );

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
		int compressAlgorithm;
		
		// almost CODEINE, wot no N
		static public final int MAGIC = 0xC0DE10E;

		// The number of bytes required for all the Header's fields
		static private final int FIELDSIZETOTAL =
			4 + 4 + 4 + 4 +		// magic, version, type, flags
			64 + 64 +			// diskid, session
			16 + 16 +			// uuid x 2
			8 + 8 +				// capacity, grainSize
			4 + 8 + 8 + 8 +		// numGTEsPerGT, offset x 3
			4;					// compressAlgorithm

		// The number of bytes allocated on disk for a Header
		static public final int SIZEOF = 512;

	}

	Header header;

	// Only one or the other is valid, never both. 
	protected File rawData;		    // for creating/writing a ManagedDisk
	protected File managedData;		// for loading a ManagedDisk

	public enum DiskTypes { ERROR, FLAT };
	
	// Tupelo Managed Disk
	static public final String FILESUFFIX = ".tmd";
	
	static public final FilenameFilter FILEFILTER =
		new FilenameFilter() {
			public boolean accept( File dir, String name ) {
				return name.endsWith( FILESUFFIX );
			}
		};

	// In sectors, NOT bytes
	static public long GRAINSIZE_DEFAULT = 128;
}

// eof
