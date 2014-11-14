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
import java.util.zip.Deflater;;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;


import org.xerial.snappy.Snappy;

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
	
	/*
	  Called by users putting unmanaged data into a store...
	*/
	public StreamOptimizedDisk( UnmanagedDisk ud, Session session,
								long grainSize ) {
		super( ud, null );

		checkGrainSize( grainSize );

		/*
		  We require the data to be managed to be a whole number of grains.
		  If the unmanaged data size does not satisfy that constraint too,
		  we must pad the managed data
		*/
		long len = unmanagedData.size();
		int padding = 0;
		
		long lenAligned = alignUp( len, grainSize * Constants.SECTORLENGTH );
		if( lenAligned != len ) {
			log.info( "Extending " + len + " -> " + lenAligned );
			padding = (int)(lenAligned - len);
		}
		checkSize( lenAligned, grainSize );

		String diskID = unmanagedData.getID();
		UUID parent = Constants.NULLUUID;
		long capacity = len2 / Constants.SECTORLENGTH;
		header = new Header( diskID, session, DiskTypes.STREAMOPTIMIZED,
							 parent, capacity, grainSize );

		grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
		grainTableCoverageBytes = grainSizeBytes * header.numGTEsPerGT;
		header.dataOffset = Header.SIZEOF;
		header.padding = padding;
		header.compressAlgorithm = Compressions.DEFLATE;
	}

	/**
	  Called from ManagedDisk.readFrom(), store side.  Note how we
	  postpone metadata read until needed (caller asks for
	  an InputStream)

	  @see #readMetaData
	  @see #getInputStream
	*/
	public StreamOptimizedDisk( File managedData, Header h )
		throws IOException {
		super( null, managedData );
		header = h;
		grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
		grainTableCoverageBytes = grainSizeBytes * header.numGTEsPerGT;
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


	// Synchronized in case of concurrent access in a web-based store...
	private synchronized void readMetaData() throws IOException {
		if( zeroGrain != null ) {
			return;
		}
	    if( header.grainSize == GRAINSIZE_DEFAULT ) {
			zeroGrain =	ZEROGRAIN_DEFAULT;
			zeroGrainTable = ZEROGRAINTABLE_DEFAULT;
		} else {
			zeroGrain = new byte[(int)grainSizeBytes];
			zeroGrainTable = new byte[(int)grainTableCoverageBytes];
		}
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

		// sanity check, locate the GD marker, precedes the GD
		if( true ) {
			raf.seek( footer.gdOffset * Constants.SECTORLENGTH -
					  Constants.SECTORLENGTH );
			MetadataMarker mdm = MetadataMarker.readFrom( raf );
			log.debug( "Expected GD: actual " + mdm );
		}
		
		long grainCount = footer.capacity / footer.grainSize;
		int grainTableCount = (int)alignUp(grainCount, footer.numGTEsPerGT) /
			footer.numGTEsPerGT;
		long[] gdes = new long[grainTableCount];
		raf.seek( footer.gdOffset * Constants.SECTORLENGTH );
		for( int i = 0; i < gdes.length; i++ ) {
			long gde = raf.readInt() & 0xffffffffL;
			log.debug( i + " " + gde );
			gdes[i] = gde;
		}
		grainDirectory = new long[grainTableCount][];
		for( int i = 0; i < gdes.length; i++ ) {
			long gde = gdes[i];
			
			if( gde == 0 ) {
				grainDirectory[i] = ZEROGDE;
				continue;
			}
			if( gde == -1 ) {
				grainDirectory[i] = PARENTGDE;
				continue;
			}
			
			// sanity check, locate the GT marker, precedes the GT
			if( true ) {
				raf.seek( gde * Constants.SECTORLENGTH -
						  Constants.SECTORLENGTH );
				MetadataMarker mdm = MetadataMarker.readFrom( raf );
				log.debug( "Expected GT: actual " + mdm );
			}
			
			raf.seek( gde * Constants.SECTORLENGTH );
			long[] grainTable = new long[footer.numGTEsPerGT];
			for( int gt = 0; gt < grainTable.length; gt++ ) {
				long gte = raf.readInt() & 0xffffffffL;
				grainTable[gt] = gte;
			}
			grainDirectory[i] = grainTable;
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
	@Override
	public void writeTo( OutputStream os ) throws IOException {
		if( unmanagedData == null )
			throw new IllegalStateException
				( header.diskID + ": unmanagedData null" );
		InputStream is = unmanagedData.getInputStream();
		readFromWriteTo( is, os );
		is.close();
	}
	
	/**
	 * @param is An InputStream implementation likely to be
	 * participating in a byte count operation for ProgressMonitor
	 * purposes.  All data is to be read from this stream, NOT from
	 * the result of the StreamOptimizedDisk's own
	 * ManagedDisk.getInputStream().
	 *
	 * Unlike FlatDisk however, our own writeTo( OutputStream ) calls
	 * this, since the write operation is involved and we do NOT want
	 * to duplicate it.
	 */
	@Override
	public void readFromWriteTo( InputStream is, OutputStream os )
		throws IOException {
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

		// In pathological cases, the compression expands input!
		byte[] compressedGrainBuffer = new byte[(int)(2*grainSizeBytes)];

		/*
		  So that all data structures line up on a sector boundary in the
		  managed file, we pad where necessary (so use a subset of this)
		*/
		byte[] padding = new byte[Constants.SECTORLENGTH];
		/*
		  Remember: any offset in a GD or GT is an offset (in sectors)
		  into the _managed_ data file.  The 'lba' values assigned to
		  compressed grain marker represent offsets (in sectors) in
		  the _unmanaged_ (i.e. real) data
		*/
		log.info( "Grain Tables " + grainTableCount );
		int wholeGrainTables = (int)(grainCount / header.numGTEsPerGT );
		log.info( "Whole Grain Tables " + wholeGrainTables  );
		if( wholeGrainTables > 0 ) {
			byte[] ba = new byte[(int)grainTableCoverageBytes];
			//			BufferedInputStream bis = new BufferedInputStream( is, ba.length );
			InputStream bis = is;
			for( int gt = 0; gt < wholeGrainTables; gt++ ) {
				log.debug( "GDIndex " + gdIndex );
				int nin = bis.read( ba );
				if( nin != ba.length )
					throw new IllegalStateException( "Partial read!" );
				gtIndex = 0;

				if( true ) {
					// The whole grain table could be zeros...
					boolean allZeros = true;
					for( int b = 0; b < ba.length; b++ ) {
						if( ba[b] != 0 ) {
							//		log.debug( "!Zero GD at " + b );
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
				}
				
				// Some grains in the table could be zeros...
				for( int g = 0; g < grainTable.length; g++ ) {
					int offset = (int)(grainSizeBytes * g);
					log.debug( "GT Offset " + offset );
					boolean allZeros = true;
					for( int b = 0; b < grainSizeBytes; b++ ) {
						if( ba[offset+b] != 0 ) {
							//log.debug( "!Zero GT at " + b );
							allZeros = false;
							break;
						}
					}
					if( allZeros ) {
						log.debug( "Zero GT at " + gdIndex + " " + gtIndex );
						grainTable[gtIndex] = 0;
						gtIndex++;
						lba += header.grainSize;
						continue;
					}

					// This grain is not zeros, compress
					int compressedLength =
						compressGrain( ba, offset, (int)grainSizeBytes,
									   compressedGrainBuffer );
					log.debug( "Deflating " + gt + " "+ g +
								  " = " + compressedLength );


					/*
					  Record in the grain table where in the managed
					  data this compressed grain sits
					*/
					grainTable[gtIndex] = dos.size() / Constants.SECTORLENGTH;
					GrainMarker gm = new GrainMarker( lba, compressedLength );
					gm.writeTo( dos );
					dos.write( compressedGrainBuffer, 0, compressedLength );
					long written = GrainMarker.SIZEOF + compressedLength;
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
				dos.flush();


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
			log.debug( "GD: " + gde + " "+ grainDirectory[gde] );
			dos.writeInt( (int)grainDirectory[gde] );
		}
		int written = 4 * grainDirectory.length;
		int padLen = (int)alignUp( written, Constants.SECTORLENGTH ) - written;
		dos.write( padding, 0, padLen );
		
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
		dos.flush();
		//dos.close();
		//is.close();
	}

	/**
	 * The best a StreamOptimizedDisk can do to verify that a file on
	 * disk really is the managed representation of the associated
	 * unmanaged data is to assert the following:
	 *
	 * The last-but-one sector is a valid Header (has magic number
	 * etc).  Recall the 'footer', a copy of the Header but with true
	 * grainDirectory offset, is added during the write.
	 *
	 * The final sector of the file is all zeros, this denotes the EOS
	 * marker added during the write.
	 *
	 * Note how this impl does NOT read any unmanaged/managed content,
	 * only StreamOptimizedDisk meta-data.  We could compare unmanaged
	 * + managed content, but how much ? Random sectors?  And we'd
	 * have to uncompress grains too. We're asserting here that if the
	 * meta-data looks OK, the unmanaged -> managed transfer went OK.
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public void verify() throws IOException  {
		if( managedData == null )
			throw new IllegalStateException( "Verify failed. noManagedData" );
		RandomAccessFile raf = new RandomAccessFile( managedData, "r" );
		try {
			raf.seek( raf.length() - 2 * Constants.SECTORLENGTH );
			Header h = new Header( raf );
			byte[] ba = new byte[Constants.SECTORLENGTH];
			raf.readFully( ba );
			for( int i = 0; i < ba.length; i++ ) {
				if( ba[i] != 0 )
					throw new IllegalStateException( "Verify failed: Bad EOS" );
			}
		} finally {
			raf.close();
		}
	}

	/**
	 * @return The compressed data count, in bytes
	 */
	private int compressGrain( byte[] ba, int offset, int len, byte[] output )
		throws IOException {
		int result = 0;
		switch( header.compressAlgorithm ) {
		case DEFLATE:
			Deflater def = new Deflater();// Deflater.BEST_COMPRESSION );
			def.setInput( ba, offset, len );
			def.finish();
			result = def.deflate( output );
			def.end();
			break;
		case GZIP:
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream( baos );
			gzos.write( ba, offset, len );
			gzos.finish();
			//			gzos.close();
			byte[] ba2 = baos.toByteArray();
			System.arraycopy( ba2, 0, output, 0, ba2.length );
			result = ba2.length;
			/*
			  System.out.println( result );
			if( true )
					System.exit(0);
			*/
			break;
		case SNAPPY:
			result = Snappy.compress( ba, offset, len, output, 0 );
			break;
		}
		return result;
	}
	
	/**
	 * @return The uncompressed data count, in bytes
	 */
	private int uncompressGrain( byte[] ba, int offset, int len, byte[] output )
		throws DataFormatException, IOException {
		int result = 0;
		switch( header.compressAlgorithm ) {
		case DEFLATE:
			Inflater inf = new Inflater();
			inf.setInput( ba, offset, len );
			result = inf.inflate( output );
			inf.end();
			break;
		case GZIP:
			ByteArrayInputStream bais = new ByteArrayInputStream( ba );
			GZIPInputStream gzis = new GZIPInputStream( bais );
			int total = 0;
			while( total < output.length ) {
				int nin = gzis.read( output, total, output.length - total );
				//				System.out.println( nin + " " + total );
				if( nin == -1 )
					break;
				total += nin;
			}
			gzis.close();
			result = total;
			break;
		case SNAPPY:
			if( true ) {
				if( !Snappy.isValidCompressedBuffer( ba, offset, len ) )
				throw new DataFormatException( "!isValidCompressedBuffer" );
			}
			result = Snappy.uncompress( ba, offset, len, output, 0 );
			// to do
			break;
		}
		return result;
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
		readMetaData();
		return new SODRandomAccessRead( size() );
	}

	@Override
	public SeekableInputStream getSeekableInputStream() throws IOException {
		readMetaData();
		return new SODRandomAccessRead( size() );
	}

	class SODRandomAccessRead extends SeekableInputStream {
		SODRandomAccessRead( long size ) throws IOException {
			super( size );
			raf = new RandomAccessFile( managedData, "r" );
			log2GrainSize = log2( grainSizeBytes );
			log2GrainTableCoverage = log2( grainTableCoverageBytes );
			//			log.info( grainSizeBytes + " " + grainTableSizeBytes );
			log.info( "log2 " + log2GrainSize + " " + log2GrainTableCoverage );
			/*
			  Twice a grain since in pathological cases, compression
			  actually EXPANDS the grain!
			*/
			compressedGrainBuffer = new byte[(int)(2*grainSizeBytes)];
			grainBuffer = new byte[(int)grainSizeBytes];
			dPos();
		}

		@Override
		public void close() throws IOException {
			raf.close();
		}
		   
		@Override
		public void seek( long s ) throws IOException {
			// according to java.io.RandomAccessFile, no restriction on seek
			posn = s;
			dPos();
		}

		@Override
		public long skip( long n ) throws IOException {
			long result = super.skip( n );
			dPos();
			return result;
		}
		
		@Override
		public int readImpl( byte[] ba, int off, int len ) throws IOException {

			log.debug( "Posn " + posn + " len  " + len );

			// do min in long space, since size - posn may overflow int...
			long actualL = Math.min( size - posn, len );

			// Cannot blindly coerce a long to int, could be -ve
			int actual = actualL > Integer.MAX_VALUE ? Integer.MAX_VALUE :
				(int)actualL;

			//logger.debug( "Actual " + actualL + " " + actual );
			int total = 0;
			while( total < actual ) {
				int left = actual - total;
				long[] gde = grainDirectory[gdIndex];
				if( false ) {
				} else if( gde == ZEROGDE ) {
					log.debug( "Zero GD : "+ gdIndex );
					int grainTableOffset = (int)
						(gtIndex * grainSizeBytes + gOffset);
					int inGrainTable = (int)
						(grainTableCoverageBytes - grainTableOffset);
					int fromGrainTable = Math.min( left, inGrainTable );
					System.arraycopy( zeroGrainTable, grainTableOffset,
									  ba, off+total, fromGrainTable );
					log.debug( len + " " + actual + " " +
							   left + " " + inGrainTable + " " +
							   fromGrainTable );
					total += fromGrainTable;
					posn += fromGrainTable;
				} else if( gde == PARENTGDE ) {
					throw new IllegalStateException( "PARENTGDE!" );
				} else {
					int inGrain = (int)(grainSizeBytes - gOffset );
					int fromGrain = Math.min( left, inGrain );
					log.debug( len + " " + actual + " " + left + " " +
							   inGrain +
							   " " + fromGrain );
					long grainMarker = gde[gtIndex];
					if( grainMarker == 0 ) {
						log.debug( "Zero GT : "+ gdIndex + " " + gtIndex );
						System.arraycopy( zeroGrain, 0,
										  ba, off+total, fromGrain );
					} else {
						// LOOK: same as last grain accessed ???
						raf.seek( grainMarker * Constants.SECTORLENGTH );
						GrainMarker gm = GrainMarker.readFrom( raf );
						int nin = raf.read( compressedGrainBuffer,
											0, gm.size );
						if( nin != gm.size )
							throw new IllegalStateException
								( "Partial read: "+ nin + " " + gm.size);
						log.debug( "Inflating " + gdIndex + " "+ gtIndex +
								  " = " + nin + " " + gm.lba );
						try {
							int actualLength = uncompressGrain
								( compressedGrainBuffer, 0, nin, grainBuffer );
							if( actualLength != grainSizeBytes )
								throw new IllegalStateException
									( "Bad inflate len: " + actualLength );
							System.arraycopy( grainBuffer, (int)gOffset,
											  ba, off+total, fromGrain );
						} catch( DataFormatException dfe ) {
							// now what ???
							log.warn( dfe );
						}
					}
					total += fromGrain;
					posn += fromGrain;
				}
				log.debug( total + " " + posn );
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

			// Note: All arithmetic using shifts/masks not multiply/divide !!
			
			gdIndex = (int)(posn >>> log2GrainTableCoverage);
			long inTable = posn - ((long)gdIndex << log2GrainTableCoverage);
			gtIndex = (int)(inTable >>> log2GrainSize);
			gOffset = inTable & (grainSizeBytes - 1);

			if( log.isDebugEnabled() )
				log.debug( "gdIndex: " + gdIndex +
							  " gtIndex: " + gtIndex +
							  " gOffset: " + gOffset );
		}
	
		private final RandomAccessFile raf;
		private int log2GrainSize, log2GrainTableCoverage;
		private byte[] compressedGrainBuffer;
		private byte[] grainBuffer;
		private int gdIndex, gtIndex;
		private long gOffset;
	}
	
	static int log2( long i ) {
		for( int p = 0; p < 32; p++ ) {
			if( i == 1 << p )
				return p;
		}
		throw new IllegalArgumentException( "Not a pow2: " + i );
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
		static GrainMarker readFrom( DataInput di ) throws IOException {
			long lba = di.readLong();
			int size = di.readInt();
			return new GrainMarker( lba, size );
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
	private long grainSizeBytes, grainTableCoverageBytes;
	private byte[] zeroGrain;
	private byte[] zeroGrainTable;
	private long[][] grainDirectory;

	
	static private final byte[] ZEROGRAIN_DEFAULT =
		new byte[(int)(GRAINSIZE_DEFAULT * Constants.SECTORLENGTH)];

	static private final byte[] ZEROGRAINTABLE_DEFAULT =
		new byte[(int)(GRAINSIZE_DEFAULT * Constants.SECTORLENGTH *
					   NUMGTESPERGT )];

	static private long[] ZEROGDE = new long[0];
	static private long[] PARENTGDE = new long[0];
}

// eof
