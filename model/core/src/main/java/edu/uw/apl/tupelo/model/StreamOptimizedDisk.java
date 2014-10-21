package edu.uw.apl.tupelo.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.io.FileUtils;

/**
 * Named after VMWare's own 'stream optimized sparse extent', a way of
 * composing, transmitting and storing virtual machine hard disk
 * (.vmdk) data efficiently, and as used in .ovf files.
 *
 * @see https://www.vmware.com/support/developer/vddk/vmdk_50_technote.pdf
 */

public class StreamOptimizedDisk extends ManagedDisk {

	public StreamOptimizedDisk( UnmanagedDisk ud, Session session ) {
		this( ud, session, GRAINSIZE_DEFAULT );
	}
	
	public StreamOptimizedDisk( UnmanagedDisk ud, Session session,
								long grainSize ) {
		super( ud, null );

		checkGrainSize( grainSize );
		// We require the data to be managed to be a whole number of grains...
		long len = unmanagedData.size();
		checkSize( len, grainSize );

		String diskID = unmanagedData.getID();
		UUID parent = Constants.NULLUUID;
		long capacity = len / Constants.SECTORLENGTH;
		header = new Header( diskID, session, DiskTypes.STREAMOPTIMIZED,
							 parent, capacity, grainSize );

		// dataOffset essentially meaningless for this type of data layout...
		header.dataOffset = 0;
	}
	

	// Called from ManagedDisk.readFrom()
	public StreamOptimizedDisk( File managedData, Header h ) throws IOException {
		super( null, managedData );
		header = h;
		zeroGrain = new byte[(int)(header.grainSize * Constants.SECTORLENGTH)];
		readMetaData();
	}

	// We require the grainSize to be power of 2 (for sparse disk arithmetic)
	private void checkGrainSize( long grainSize ) {
		boolean found = false;
		for( int i = 3; i < 32; i++ ) {
			if( grainSize == (1L << i) ) {
				found = true;
				break;
			}
		}
		if( !found )
			throw new IllegalArgumentException( "grainSize not 2^N: " +
												grainSize );
	}
			
		
	// We require the managed data to be a whole number of grains...
	private void checkSize( long advertisedSizeBytes, long grainSize ) {
		long grainSizeBytes = grainSize * Constants.SECTORLENGTH;
		if( advertisedSizeBytes % grainSizeBytes != 0 ) {
			throw new IllegalArgumentException
				( "Data length (" + advertisedSizeBytes +
				  ") must be a multiple of " + grainSizeBytes );
		}
	}


	private void readMetaData() throws IOException {
		RandomAccessFile raf = new RandomAccessFile( managedData, "r" );
		// the managed data ends with footer and eos marker, each 1 sector long
		long footerOffset = raf.length() - (2 * Constants.SECTORLENGTH );
		raf.seek( footerOffset );
		byte[] ba = new byte[Constants.SECTORLENGTH];
		raf.readFully( ba );
		ByteArrayInputStream bais = new ByteArrayInputStream( ba );
		Header footer = new Header( bais );
		log.info( "Footer.gdOffset: " + footer.gdOffset );
		bais.close();

		if( true ) {
			raf.seek( footer.gdOffset * Constants.SECTORLENGTH -
					  Constants.SECTORLENGTH );
			MetadataMarker mdm = MetadataMarker.readFrom( raf );
			log.info( "Expected GD: actual " + mdm );
		}
		
		long grainCount = footer.capacity / footer.grainSize;
		int grainTableCount = (int)alignUp(grainCount, footer.numGTEsPerGT) /
			footer.numGTEsPerGT;
		grainDirectory = new long[grainTableCount];
		grainTables = new long[grainTableCount][];
		raf.seek( footer.gdOffset * Constants.SECTORLENGTH );
		for( int i = 0; i < grainDirectory.length; i++ ) {
			long gde = raf.readInt() & 0xffffffffL;
			grainDirectory[i] = gde;
			if( gde == 0 )
				continue;
			log.info( i + " " + gde );
		}

		for( int i = 0; i < grainDirectory.length; i++ ) {
			long gde = grainDirectory[i];
			if( gde == 0 )
				continue;
			long[] grainTable = new long[footer.numGTEsPerGT];

			if( true ) {
				raf.seek( gde * Constants.SECTORLENGTH - Constants.SECTORLENGTH );
				MetadataMarker mdm = MetadataMarker.readFrom( raf );
				log.info( "Expected GT at " + i + ": actual " + mdm );
			}

			raf.seek( gde * Constants.SECTORLENGTH );
			for( int gt = 0; gt < grainTable.length; gt++ ) {
				long gte = raf.readInt() & 0xffffffffL;
				grainTable[gt] = gte;
			}
			grainTables[i] = grainTable;
		}
		raf.close();
	}
	
	@Override
	public void setParent( ManagedDisk md ) {
		parent = md;
	}

	/*
	  The constructor has already verified that the unmanaged/input
	  data length is a whole number of grains.  We further check (and
	  expect) that it is also a whole number of grains * NUMGTESPERGT.
	  If so, we can then read in chunks sized to cover an entire grain
	  table.  Then, if we find all zeros, we can completely omit that
	  grain table and set that grain directory entry to 0.
	*/
	public void writeTo( OutputStream os ) throws IOException {
		if( unmanagedData == null )
			throw new IllegalStateException
				( header.diskID + ": unmanagedData null" );
		InputStream is = unmanagedData.getInputStream();

		DataOutputStream dos = new DataOutputStream( os );
		header.writeTo( (DataOutput)dos );
		
		long grainCount = header.capacity / header.grainSize;
		int grainTableCount = (int)alignUp(grainCount, header.numGTEsPerGT ) /
			header.numGTEsPerGT;
		long[] grainDirectory = new long[grainTableCount];
		long[] grainTable = new long[header.numGTEsPerGT];
		int gdIndex = 0;
		int gtIndex = 0;
		long lba = 0;

		/*
		  So that all data structures line up on a sector boundary in the
		  managed file, we pad where necessary (so use a subset of this)
		*/
		byte[] padding = new byte[Constants.SECTORLENGTH];
		/*
		  Remember: any offset in a GD or GT is an offset (in sectors)
		  into the _managed_ data file.  The 'lba' values assiged to
		  compressed grain marker represent offsets (in sectors) in
		  the _unmanaged_ (i.e. real) data
		*/
		log.info( "Grain Tables " + grainTableCount );
		int wholeGrainTables = (int)(grainCount / header.numGTEsPerGT );
		log.info( "Whole Grain Tables " + wholeGrainTables  );
		if( wholeGrainTables > 0 ) {
			long grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
			long grainTableCoverageBytes = grainSizeBytes * header.numGTEsPerGT;
			log.debug( "Grain Table Coverage Bytes " + grainTableCoverageBytes  );
			byte[] ba = new byte[(int)grainTableCoverageBytes];
			BufferedInputStream bis = new BufferedInputStream( is, ba.length );
			for( int gt = 0; gt < wholeGrainTables; gt++ ) {
				log.info( "GDIndex " + gdIndex );
				int nin = bis.read( ba );
				if( nin != ba.length )
					throw new IllegalStateException( "Partial read!" );
				gtIndex = 0;
				// The whole grain table could be zeros...
				boolean allZeros = true;
				for( int b = 0; b < ba.length; b++ ) {
					if( ba[b] != 0 ) {
						//						log.debug( "!Zero GD at " + b );
						allZeros = false;
						break;
					}
				}
				if( allZeros ) {
					log.debug( "Zero GDE at " + gdIndex );
					grainDirectory[gdIndex] = 0;
					gdIndex++;
					lba += header.grainSize * header.numGTEsPerGT;
					continue;
				}
				
				// Some grains in the table could be zeros...
				for( int g = 0; g < grainTable.length; g++ ) {
					int offset = (int)(grainSizeBytes * g);
					log.debug( "GT Offset " + offset );
					allZeros = true;
					for( int b = 0; b < grainSizeBytes; b++ ) {
						if( ba[offset+b] != 0 ) {
							//						log.debug( "!Zero GT at " + b );
							allZeros = false;
							break;
						}
					}
					if( allZeros ) {
						log.debug( "Zero GT at " + gtIndex );
						grainTable[gtIndex] = 0;
						gtIndex++;
						lba += header.grainSize;
						continue;
					}

					// This grain is not zeros, compress
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DeflaterOutputStream dfos = new DeflaterOutputStream( baos );
					dfos.write( ba, offset, (int)grainSizeBytes );
					byte[] compressed = baos.toByteArray();
					log.debug( "Compressed: " + compressed.length );

					/*
					  Record in the grain table where in the managed
					  data this compressed grain sits
					*/
					grainTable[gtIndex] = dos.size() / Constants.SECTORLENGTH;
					GrainMarker gm = new GrainMarker( lba, compressed.length );
					gm.writeTo( dos );
					dos.write( compressed );
					long written = GrainMarker.SIZEOF + compressed.length;
					// to do: pad to next sector of os
					int padLen = (int)
						(alignUp( written, Constants.SECTORLENGTH ) - written);
					dos.write( padding, 0, padLen );
					log.debug( "Padding: " + padLen );
					gtIndex++;
					lba += header.grainSize;
				}

				/*
				  A table's worth of grains just written, next comes
				  the grain table describing them (their locations in
				  the managed data)
				*/
				int fullGrainTableSizeSectors = 4 * header.numGTEsPerGT
					/ Constants.SECTORLENGTH;
				MetadataMarker mdm = new MetadataMarker
					( fullGrainTableSizeSectors, MetadataMarker.TYPE_GT );
				mdm.writeTo( dos );


				/*
				  We mark where the grain table itself is, NOT its marker,
				  and record this offset in the grain directory
				*/
				long gtOffset = dos.size() / Constants.SECTORLENGTH;
				for( int gte = 0; gte < grainTable.length; gte++ ) {
					// LOOK: Using 4byte GTE index values restricts us to 2TB
					dos.writeInt( (int)grainTable[gte] );
				}
				grainDirectory[gdIndex] = gtOffset;
				gdIndex++;
				/*
				  For full grain tables, no padding needing since the
				  managed data file still sector aligned (the
				  metadatamarker AND the grain table data both sector
				  multiples)
				*/
			}
		}
			
		int trailingGrains = (int)(grainCount -
								   (wholeGrainTables * header.numGTEsPerGT));
		if( trailingGrains > 0 ) {
			log.info( "Trailing Grains " + trailingGrains  );
			// to do
		}

		// The grain directory (preceded by its marker) follows the last grain table...
		long grainDirectorySizeSectors =
			alignUp( 4 * grainDirectory.length,
					 Constants.SECTORLENGTH ) /	Constants.SECTORLENGTH;
		log.info( "Graindirectorysizesectors: " + grainDirectorySizeSectors );
		MetadataMarker mdm = new MetadataMarker
			( grainDirectorySizeSectors, MetadataMarker.TYPE_GD );
		mdm.writeTo( dos );

		// Record the gd offset so we can place it in the footer...
		long gdOffset = dos.size() / Constants.SECTORLENGTH;
		log.info( "Footer gdOffset: " + gdOffset );

		// This is the grain directory write...
		for( int gde = 0; gde < grainDirectory.length; gde++ ) {
			dos.writeInt( (int)grainDirectory[gde] );
		}

		/*
		  The footer (a copy of the header) follows the grain
		  directory (after its own marker that is).  The footer's
		  gdOffset is set to the grain directory offset, which we
		  know.  We'll serialize the header and read it back in to
		  create the footer (LOOK: provide a copy constructor??)
		*/
		
		mdm = new MetadataMarker( 1, MetadataMarker.TYPE_FOOTER );
		mdm.writeTo( dos );
		
		ByteArrayOutputStream hdr = new ByteArrayOutputStream();
		header.writeTo( hdr );
		ByteArrayInputStream ftr = new ByteArrayInputStream( hdr.toByteArray() );
		Header footer = new Header( ftr );
		footer.gdOffset = gdOffset;
		footer.writeTo( (DataOutput)dos );
		
		// Finally, the end-of-stream marker
		mdm = new MetadataMarker( 0, MetadataMarker.TYPE_EOS );
		mdm.writeTo( dos );
					 
		log.info( "Written " + dos.size() );
		dos.close();
		is.close();
	}
	

	public void writeTo( File f ) throws IOException {
		FileOutputStream fos = new FileOutputStream( f );
		long defaultGrainTableCoverageBytes = GRAINSIZE_DEFAULT *
			Constants.SECTORLENGTH * NUMGTESPERGT;
		BufferedOutputStream bos = new BufferedOutputStream
			( fos, (int)defaultGrainTableCoverageBytes );
		writeTo( bos );
		bos.close();
		fos.close();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new SODInputStream( size() );
	}

	@Override
	public RandomAccessRead getRandomAccessRead() throws IOException {
		return null;
	}


	class SODInputStream extends ManagedDiskInputStream {
		SODInputStream( long size ) throws IOException {
			super( size );
			raf = new RandomAccessFile( managedData, "r" );
			grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
			grainTableCoverageBytes = grainSizeBytes * header.numGTEsPerGT;
			posn = 0;
			dPos();
		}

		@Override
		public void close() throws IOException {
			raf.close();
		}
		   
		@Override
		public int readImpl( byte[] ba, int off, int len ) throws IOException {

			// do min in long space, since size - posn may overflow int...
			long actualL = Math.min( size - posn, len );

			// LOOK: check int.max_value, else could get -ve by casting
			int actual = (int)actualL;

			//logger.debug( "Actual " + actualL + " " + actual );
			int total = 0;
			while( total < actual ) {
				int left = actual - total;
				int inGrain = (int)(grainSizeBytes - gOffset );
				int fromGrain = Math.min( left, inGrain );
				// LOOK: turn the switch into ifs, since cannot switch on long ??
				int gde = (int)grainDirectory[gdIndex];
				switch( gde ) {
				case 0:
					System.arraycopy( zeroGrain, 0, ba, off+total, fromGrain );
					break;
				default:
					// to do
					;
				}
				total += fromGrain;
				posn += fromGrain;
				dPos();
			}
			return total;
		}

		/**
		   Called whenever the local posn changes value.
		   Do NOT make calls to the parent.dPos here,
		   but only from the reads...
		*/
		private void dPos() {
			/*
			  This is the crux of the sparse and stream optimized
			  reading. We map logically map the 'file pointer' on the
			  input stream to a grain table, table entry and grain
			  offset.  To do the next read, we then calc a physical
			  seek offset into the actual managed data file and read.

			  According to java.io.RandomAccessFile, a file posn
			  is permitted to be past its size limit.  We cannot
			  map such a posn to the grain info of course...
			*/
			if( posn >= size )
				return;

			// LOOK: use pow2 here to eliminate / and % ops !!
			
			long unmanagedOffset = posn;
			gdIndex = (int)(posn / grainTableCoverageBytes);
			// this next operation MUST be done in long space, NOT int...
			gtIndex = (int)((posn - grainTableCoverageBytes * gdIndex) /
							grainSizeBytes);
			gOffset = (int)(posn % grainSizeBytes);

			if( log.isDebugEnabled() )
				log.debug( "gdIndex: " + gdIndex +
							  " gtIndex: " + gtIndex +
							  " gOffset: " + gOffset );
		}

	
		private final RandomAccessFile raf;
		private final long grainSizeBytes, grainTableCoverageBytes;
		private int gdIndex, gtIndex, gOffset;
	}
	
	/**
	   struct GrainMarker {
	     long lba;
		 int size;
	   }
	*/
	static class GrainMarker {
		GrainMarker( long lba, int size ) {
			this.lba = lba;
			this.size = size;
		}
		void writeTo( DataOutputStream dos ) throws IOException {
			dos.writeLong( lba );
			dos.writeInt( size );
		}
		final long lba;
		final int size;

		static final int SIZEOF = 8 + 4;
	}

	/**
	   struct MetadataMarker {
	     long numSectors;
		 int size = 0;
		 int type;
	   }
	*/
	static class MetadataMarker {
		MetadataMarker( long numSectors, int type ) {
			this.numSectors = numSectors;
			this.type = type;
		}
		void writeTo( DataOutputStream dos ) throws IOException {
			dos.writeLong( numSectors );
			dos.writeInt( SIZE );
			dos.writeInt( type );
			dos.write( PADDING );
		}
		static MetadataMarker readFrom( DataInput di ) throws IOException {
			long numSectors = di.readLong();
			int size = di.readInt();
			// LOOK: check size is zero
			int type = di.readInt();
			return new MetadataMarker( numSectors, type );
		}

		@Override
		public String toString() {
			return "" + numSectors + "," + type;
		}
		
			   
		final long numSectors;
		final int type;

		// Three fields on disk (the 'size' field is implicit and set to 0)
		static final int SIZEOF = 8 + 4 + 4;

		/*
		  When written out to the managed data file, all markers are
		  padded to a sector boundary.  That way, the metadata they
		  describe (grain table, grain directory, etc) starts on its
		  own sector boundary
		*/

		static final int SIZE = 0;
		static final byte[] PADDING = new byte[Constants.SECTORLENGTH-SIZEOF];
		
		static final int TYPE_EOS = 0;
		static final int TYPE_GT = 1;
		static final int TYPE_GD = 2;
		static final int TYPE_FOOTER = 3;
	}
	
	private ManagedDisk parent;
	private byte[] zeroGrain;
	private long[] grainDirectory;
	private long[][] grainTables;
}

// eof
