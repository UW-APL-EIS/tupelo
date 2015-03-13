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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
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

/**
   LOOK: can assert that unmanaged data size be whole number of
   SECTORS but that is all.  Store THAT number in header.capacity.
   Grains needed is then alignUp( capacity, grainSize ) * grainSize,
   so min grains = 1 Then grainTables needed is alignUp( grainsNeeded,
   numgtespergt ) * numgtespergt, so min grainTables also 1.

   size is always header.capacity * 512, and size v important since
   used by inputstreams, seekableinputstream.

   Now, in writing, we have to write a whole number of grains, even
   though the lasrt grain may not be able to be 'filled' with
   unmanaged data, since it may be at eof.

   Do we need to store 512 grains to 'fill' a grain table, or can we
   just store 1 - 511 grains and THEN the grain table, with some of
   its entries zero/bogus??  should be a yes, since those gt entries
   should never be accessed

   So, on writing the SODisk:

   compute grainCount, grainTableCount, grainDirectoryLength

   write all 'full grain tables' as we have now, then

   write all full grains, enter locations in last gt

   write remaining sectors, padded to a grain, then compressed.  W/out
   the padding, will not uncompress to a whole grain, giving us a
   special case we would like to avoid. enter location in last gt

   pad to 512 entries and write last gt.  write location of this gt into gd

   write grain directory

   On read, should be no special logic. Just must make sure we never
   access a grain table with an 'undefined' offset.  Also, must make
   sure not to read past eof in the last grain.
*/


   
public class StreamOptimizedDisk extends ManagedDisk {

	public StreamOptimizedDisk( UnmanagedDisk ud, Session session ) {
		this( ud, session, Constants.NULLUUID, GRAINSIZE_DEFAULT );
	}
	
	public StreamOptimizedDisk( UnmanagedDisk ud, Session session,
								UUID parentUUID ) {
		this( ud, session, parentUUID, GRAINSIZE_DEFAULT );
	}
	
	/*
	  Called by users putting unmanaged data into a store...
	*/
	public StreamOptimizedDisk( UnmanagedDisk ud, Session session,
								UUID parentUUID, long grainSize ) {

		super( ud, null );

		/*
		  We require the data to be managed to be a whole number of sectors.
		*/
		checkSize( ud.size() );

		/*
		  We require that the grain size by a power of 2
		*/
		checkGrainSize( grainSize );
		
		/*
		  We require the data to be managed to be a whole number of grains.
		  If the unmanaged data size does not satisfy that constraint too,
		  we must pad the managed data
		*/
		long len = unmanagedData.size();
		int padding = 0;
		
		long lenAligned = Utils.alignUp( len,
										 grainSize * Constants.SECTORLENGTH );
		if( lenAligned != len ) {
			log.info( "Extending " + len + " -> " + lenAligned );
			padding = (int)(lenAligned - len);
		}

		String diskID = unmanagedData.getID();
		/*
		  The capacity is the number of grains required to store ALL
		  the unmanagedData, even if that unmanagedData is not a whole
		  number of grains, so capacity is a ceiling.  To derive the
		  actual data size in bytes, we subtract the padding from the
		  capacity.
		*/
		long capacityGrains = lenAligned / Constants.SECTORLENGTH;
		// Currently we have just a single sector header...
		long overhead = 1;
		header = new Header( diskID, session, DiskTypes.STREAMOPTIMIZED,
							 parentUUID, capacityGrains, grainSize,
							 overhead );

		header.padding = padding;
		header.dataOffset = Header.SIZEOF;
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
		//		grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
		//grainTableCoverageBytes = grainSizeBytes * header.numGTEsPerGT;
	}

	/**
	 * Due to possible padding involved in the Manageddata compared to
	 * its unmanaged counterpart, the size in bytes of a
	 * StreamOptimizedDisk may not always be a whole number of grains.
	 * We need an accurate size since it is used in identifying EOF in
	 * InputStreams generated from this StreamOptimizedDisk
	 */
	@Override
	public long size() {
		return header.capacity * Constants.SECTORLENGTH - header.padding;
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
		
		grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
		grainTableCoverageBytes = grainSizeBytes * header.numGTEsPerGT;
		/*
		  Recall that the final grain may be combo of actual data and
		  padding
		*/
		long grainCount = header.capacity / header.grainSize;
		int grainTableCount = (int)(Utils.alignUp(grainCount,
												  header.numGTEsPerGT ) /
									header.numGTEsPerGT);
		log.info( "GrainSize: " + header.grainSize );
		log.info( "GrainCount: " + grainCount );
		log.info( "GrainTableCount: " + grainTableCount );
		
		long[] grainDirectory = new long[grainTableCount];
		long[] grainTable = new long[header.numGTEsPerGT];
		int gdIndex = 0;
		int gtIndex = 0;
		long lba = 0;

		long zeroGDEs = 0;
		long zeroGTEs = 0;
		long parentGTEs = 0;
		int digestIndex = 0;
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance( DIGESTALGORITHM );
		} catch( NoSuchAlgorithmException never ) {
		}

		/*
		  We maintain written count in long space. DataOutputStream.size()
		  maintains only in int space, pah!  The header is already written,
		  above.
		*/
		long written = Header.SIZEOF;
		byte[] readBuffer = null;
		
		// In pathological cases, the compression expands the input!
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
		//		log.info( "Grain Tables " + grainTableCount );
		int wholeGrainTables = (int)(grainCount / header.numGTEsPerGT );
		//log.info( "Whole Grain Tables " + wholeGrainTables  );
		InputStream bis = is;
		if( wholeGrainTables > 0 ) {
			readBuffer = new byte[(int)grainTableCoverageBytes];
			//bis = new BufferedInputStream( is, readBuffer.length );
			for( int gt = 0; gt < wholeGrainTables; gt++ ) {
				log.debug( "GDIndex " + gdIndex );
				int nin = bis.read( readBuffer );
				if( nin != readBuffer.length )
					throw new IllegalStateException( "Partial read!" );
				gtIndex = 0;

				if( true ) {
					// The whole grain table could be zeros...
					boolean allZeros = true;
					for( int b = 0; b < readBuffer.length; b++ ) {
						if( readBuffer[b] != 0 ) {
							//		log.debug( "!Zero GD at " + b );
							allZeros = false;
							break;
						}
					}
					if( allZeros ) {
						zeroGDEs++;
						log.debug( "Zero GDE at " + gdIndex );
						grainDirectory[gdIndex] = 0;
						gdIndex++;
						lba += header.grainSize * header.numGTEsPerGT;
						digestIndex += header.numGTEsPerGT;
						continue;
					}
				}
				
				// Some grains in the table could be zeros...
				for( int g = 0; g < grainTable.length; g++ ) {
					int offset = (int)(grainSizeBytes * g);
					log.debug( "GT Offset " + offset );
					boolean allZeros = true;
					for( int b = 0; b < grainSizeBytes; b++ ) {
						if( readBuffer[offset+b] != 0 ) {
							//log.debug( "!Zero GT at " + b );
							allZeros = false;
							break;
						}
					}
					if( allZeros ) {
						zeroGTEs++;
						digestIndex++;
						log.debug( "Zero GT at " + gdIndex + " " + gtIndex );
						grainTable[gtIndex] = 0;
						gtIndex++;
						lba += header.grainSize;
						continue;
					}

					/*
					  This grain is not zeros. If digest available,
					  compare.  If compare satisfied, record such and
					  move on
					*/
					if( parentDigest != null ) {
						md.reset();
						md.update( readBuffer, offset, (int)grainSizeBytes );
						byte[] hash = md.digest();
						byte[] parent = parentDigest.get( digestIndex );
						if( MessageDigest.isEqual( hash, parent ) ) {
							parentGTEs++;
							grainTable[gtIndex] = -1;
							gtIndex++;
							digestIndex++;
							lba += header.grainSize;
							continue;
						}
					}

					// Grain content do not match parent grain. Compress,store
					int compressedLength =
						compressGrain( readBuffer, offset, (int)grainSizeBytes,
									   compressedGrainBuffer );
					log.debug( "Deflating " + gt + " "+ g +
								  " = " + compressedLength );


					/*
					  Record in the grain table where in the managed
					  data this compressed grain sits
					*/
					grainTable[gtIndex] = written / Constants.SECTORLENGTH;
					GrainMarker gm = new GrainMarker( lba, compressedLength );
					gm.writeTo( dos );
					dos.write( compressedGrainBuffer, 0, compressedLength );
					long grainWrite = GrainMarker.SIZEOF + compressedLength;
					int padLen = (int)(Utils.alignUp
									   ( grainWrite, Constants.SECTORLENGTH )
									   - grainWrite );
					dos.write( padding, 0, padLen );
					log.debug( "Padding: " + padLen );
					digestIndex++;
					gtIndex++;
					lba += header.grainSize;
					written += (grainWrite + padLen);

					if( written % Constants.SECTORLENGTH != 0 )
						throw new IllegalStateException( "" + written );
					
					// LOOK: check written % sectorlen == 0
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
				written += MetadataMarker.SIZEOF;

				if( written % Constants.SECTORLENGTH != 0 )
					throw new IllegalStateException( "" + written );
				/*
				  We mark where the grain table itself is, NOT its marker,
				  and record this offset in the grain directory
				*/
				long gtOffset = written / Constants.SECTORLENGTH;
				for( int gte = 0; gte < grainTable.length; gte++ ) {
					// LOOK: Using 4byte GTE index values restricts us to 2TB
					dos.writeInt( (int)grainTable[gte] );
				}
				written += 4 * grainTable.length;
				if( written % Constants.SECTORLENGTH != 0 )
					throw new IllegalStateException( "" + written );

				grainDirectory[gdIndex] = gtOffset;
				gdIndex++;
				/*
				  For full grain tables, no padding needed since the
				  managed data file still sector aligned (the
				  metadatamarker AND the grain table data both sector
				  multiples)
				*/
			}
		}

		// LOOK: we are not using any parentDigest for any remainder ??
		
		int unmanagedRemaining =
			(int)(unmanagedData.size() -
				  (wholeGrainTables * grainTableCoverageBytes));
		if( unmanagedRemaining > 0 ) {
			for( int i = 0; i < grainTable.length; i++ )
				grainTable[i] = -2;
			gtIndex = 0;
			
			log.info( "UnmanagedRemaining " + unmanagedRemaining  );
			int grainSized = (int)Utils.alignUp( unmanagedRemaining,
												 grainSizeBytes );
			/*
			  By allocating a buffer a whole number of grains,
			  the trailing padding for the last grain will be zeros
			  which is what we want
			*/
			readBuffer = new byte[grainSized];
			int nin = bis.read( readBuffer );
			if( nin != unmanagedRemaining ) {
				throw new IllegalStateException
					( "Partial read (remaining)!" );
			}
			int grainsLeft = (int)(grainSized / grainSizeBytes);
			int wholeGrains = (int)(unmanagedRemaining / grainSizeBytes);
			log.info( "Whole Grains " + wholeGrains );
			log.info( "Padded Grains " + (grainsLeft - wholeGrains) );
			for( int g = 0; g < grainsLeft; g++ ) {
				int offset = (int)(grainSizeBytes * g);

				boolean allZeros = true;
				for( int b = 0; b < grainSizeBytes; b++ ) {
					if( readBuffer[offset+b] != 0 ) {
						//log.debug( "!Zero GT at " + b );
						allZeros = false;
						break;
					}
				}
				if( allZeros ) {
					zeroGTEs++;
					log.debug( "Zero GT at " + gdIndex + " " + gtIndex );
					grainTable[gtIndex] = 0;
					gtIndex++;
					lba += header.grainSize;
					continue;
				}
				
				// This grain is not zeros, compress
				int compressedLength =
					compressGrain( readBuffer, offset, (int)grainSizeBytes,
								   compressedGrainBuffer );
				log.debug( "Deflating " + "remaining" + " " + g +
						   " = " + compressedLength );
				
				/*
				  Record in the final grain table where in the managed
				  data this compressed grain sits
				*/
				grainTable[gtIndex] = written / Constants.SECTORLENGTH;
				GrainMarker gm = new GrainMarker( lba, compressedLength );
				gm.writeTo( dos );
				dos.write( compressedGrainBuffer, 0, compressedLength );
				long grainWrite = GrainMarker.SIZEOF + compressedLength;
				// to do: pad to next sector of os
				int padLen = (int)(Utils.alignUp( grainWrite,
												  Constants.SECTORLENGTH)
								   - grainWrite );
				dos.write( padding, 0, padLen );
				log.debug( "Padding: " + padLen );
				gtIndex++;
				lba += header.grainSize;
				written += (grainWrite + padLen);

				if( written % Constants.SECTORLENGTH != 0 )
					throw new IllegalStateException( "" + written );
			}

			/*
			  The final grains just written, next comes
			  the grain table describing them (their locations in
			  the managed data).  This table is not 'full', but we
			  write it all anyway.
			*/
			int fullGrainTableSizeSectors = 4 * header.numGTEsPerGT
				/ Constants.SECTORLENGTH;
			MetadataMarker mdm = new MetadataMarker
				( fullGrainTableSizeSectors, MetadataMarker.TYPE_GT );
			mdm.writeTo( dos );
			dos.flush();
			written += MetadataMarker.SIZEOF;
			if( written % Constants.SECTORLENGTH != 0 )
				throw new IllegalStateException( "" + written );

			/*
			  We mark where the grain table itself is, NOT its marker,
			  and record this offset in the grain directory
			*/
			long gtOffset = written / Constants.SECTORLENGTH;

			// Writing the grain table
			for( int gte = 0; gte < grainTable.length; gte++ ) {
				// LOOK: Using 4byte GTE index values restricts us to 2TB
				dos.writeInt( (int)grainTable[gte] );
			}
			written += (4 * grainTable.length);
			if( written % Constants.SECTORLENGTH != 0 )
				throw new IllegalStateException( "" + written );
			grainDirectory[gdIndex] = gtOffset;
			gdIndex++;
		}
		


		/*
		  The grain directory (preceded by its marker) follows
		  the last grain table...
		*/
		long grainDirectorySizeSectors =
			Utils.alignUp( 4 * grainDirectory.length,
						   Constants.SECTORLENGTH ) /	Constants.SECTORLENGTH;
		log.info( "GrainDirectorySizeSectors: " + grainDirectorySizeSectors );
		MetadataMarker mdm = new MetadataMarker
			( grainDirectorySizeSectors, MetadataMarker.TYPE_GD );
		mdm.writeTo( dos );
		written += MetadataMarker.SIZEOF;
		if( written % Constants.SECTORLENGTH != 0 )
			throw new IllegalStateException( "" + written );
		
		// Record the gd offset so we can place it in the footer...
		long gdOffset = written / Constants.SECTORLENGTH;
		log.info( "Footer gdOffset: " + gdOffset );

		// This is the grain directory write...
		for( int gde = 0; gde < grainDirectory.length; gde++ ) {
			log.debug( "GD: " + gde + " " + grainDirectory[gde] );
			dos.writeInt( (int)grainDirectory[gde] );
		}
		written += (4 * grainDirectory.length);
		
		int gdWrite = 4 * grainDirectory.length;
		int padLen = (int)Utils.alignUp( gdWrite, Constants.SECTORLENGTH ) -
			gdWrite;
		dos.write( padding, 0, padLen );
		written += padLen;

		if( written % Constants.SECTORLENGTH != 0 )
			throw new IllegalStateException( "" + written );
		
		/*
		  The footer (a copy of the header) follows the grain
		  directory (after its own marker that is).  The footer's
		  gdOffset is set to the grain directory offset, which we
		  know.  We'll serialize the header and read it back in to
		  create the footer (LOOK: provide a copy constructor??)
		*/
		
		mdm = new MetadataMarker( 1, MetadataMarker.TYPE_FOOTER );
		mdm.writeTo( dos );
		written += MetadataMarker.SIZEOF;
		
		if( written % Constants.SECTORLENGTH != 0 )
			throw new IllegalStateException( "" + written );

		ByteArrayOutputStream hdr = new ByteArrayOutputStream();
		header.writeTo( hdr );
		ByteArrayInputStream ftr = new ByteArrayInputStream( hdr.toByteArray() );
		Header footer = new Header( ftr );
		footer.gdOffset = gdOffset;
		footer.writeTo( (DataOutput)dos );
		written += Header.SIZEOF;
		
		if( written % Constants.SECTORLENGTH != 0 )
			throw new IllegalStateException( "" + written );
		
		// Finally, the end-of-stream marker
		mdm = new MetadataMarker( 0, MetadataMarker.TYPE_EOS );
		mdm.writeTo( dos );
		written += MetadataMarker.SIZEOF;
					 
		if( written % Constants.SECTORLENGTH != 0 )
			throw new IllegalStateException( "" + written );

		log.info( "Written " + written );
		dos.flush();
		bis.close();
		//dos.close();
		//is.close();

		log.info( "ZeroGDEs: " + zeroGDEs );
		log.info( "ZeroGTEs: " + zeroGTEs );
		log.info( "ParentGTEs: " + parentGTEs );
	}



	// Synchronized in case of concurrent access in a web-based store...
	private synchronized void readMetaData() throws IOException {
		if( zeroGrain != null ) {
			return;
		}
		grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
		grainTableCoverageBytes = grainSizeBytes * header.numGTEsPerGT;
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

		/*
		  Recall that the final grain may be combo of actual data and
		  padding
		*/
		long grainCount = footer.capacity / footer.grainSize;
		int grainTableCount = (int)Utils.alignUp( grainCount,
												  footer.numGTEsPerGT ) /
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
				// stored as unsigned int OR -1, meaning 'use parent'
				long gtel;
				int gte = raf.readInt();
				if( gte == -1 )
					gtel = -1L;
				else
					gtel = gte & 0xffffffffL;
				grainTable[gt] = gtel;
			}
			grainDirectory[i] = grainTable;
		}
		raf.close();
	}

	@Override
	public String paramString() {
		try {
			readMetaData();
		} catch( IOException ioe ) {
			// LOOK:
			return "";
		}
		int zeroGDEs = 0;
		int zeroGTEs = 0;
		int parentGTEs = 0;
		
		for( long[] gt : grainDirectory ) {
			if( gt == ZEROGDE ) {
				zeroGDEs++;
				continue;
			}
			for( long gte : gt ) {
				if( gte == 0 )
					zeroGTEs++;
				else if( gte == -1 )
					parentGTEs++;
			}
		}
		return "ZeroGDEs: " + zeroGDEs + ", zeroGTEs: " + zeroGTEs +
			", parentGTEs: " + parentGTEs;
	}
	
	public void reportMetaData() throws IOException {
		readMetaData();
		for( int i = 0; i < grainDirectory.length; i++ ) {
			long[] gde = grainDirectory[i];
			log.info( "GT " + i + " " + gde.length );
			if( gde.length == 0 )
				continue;
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter( sw );
			for( int j = 0; j < gde.length; j++ ) {
				pw.print( gde[j] + " " );
			}
			pw.flush();
			log.info( sw.toString() );
		}
	}
	
			
	@Override
	public void setParentDigest( ManagedDiskDigest grainHashes ) {
		parentDigest = grainHashes;
	}
	
	@Override
	public void setParent( ManagedDisk md ) {
		parent = md;
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
		InputStream pis = parent == null ? null : parent.getInputStream();
		log.debug( "getInputStream: " + getDescriptor() );
		return new SODRandomAccessRead( (SeekableInputStream)pis );
	}

	@Override
	public SeekableInputStream getSeekableInputStream() throws IOException {
		readMetaData();
		SeekableInputStream pis = parent == null ?
			null : parent.getSeekableInputStream();
		log.debug( "getSeekableInputStream: " + pis );
		return new SODRandomAccessRead( pis );
	}

	class SODRandomAccessRead extends SeekableInputStream {
		SODRandomAccessRead( SeekableInputStream parentStream )
			throws IOException {
			super( size() );
			this.parentStream = parentStream;
			raf = new RandomAccessFile( managedData, "r" );
			log2GrainSize = log2( grainSizeBytes );
			log2GrainTableCoverage = log2( grainTableCoverageBytes );
			//			log.info( grainSizeBytes + " " + grainTableSizeBytes );
			//log.info( "log2 " + log2GrainSize + " " + log2GrainTableCoverage );
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
			if( parentStream != null )
				parentStream.close();
			raf.close();
		}
		   
		@Override
		public void seek( long s ) throws IOException {
			/*
			  According to java.io.RandomAccessFile, no restriction on
			  seek.  That is, seek posn can be -ve or past eof
			*/
			if( parentStream != null )
				parentStream.seek( s );
			posn = s;
			dPos();
		}

		@Override
		public long skip( long n ) throws IOException {
			if( parentStream != null )
				parentStream.skip( n );
			long result = super.skip( n );
			dPos();
			return result;
		}
		
		@Override
		public int readImpl( byte[] ba, int off, int len ) throws IOException {

			if( log.isDebugEnabled() )
				log.debug( "Posn " + posn + " len  " + len );

			// do min in long space, since size - posn may overflow int...
			long actualL = Math.min( size - posn, len );

			// Cannot blindly coerce a long to int, result could be -ve
			int actual = actualL > Integer.MAX_VALUE ? Integer.MAX_VALUE :
				(int)actualL;

			//logger.debug( "Actual " + actualL + " " + actual );
			int total = 0;
			while( total < actual ) {
				int left = actual - total;
				long[] gt = grainDirectory[gdIndex];
				if( false ) {
				} else if( gt == ZEROGDE ) {
					if( log.isDebugEnabled() )
						log.debug( "Zero GD : " + gdIndex );
					int grainTableOffset = (int)
						(gtIndex << log2GrainSize + gOffset);
					int inGrainTable = (int)
						(grainTableCoverageBytes - grainTableOffset);
					int fromGrainTable = Math.min( left, inGrainTable );
					System.arraycopy( zeroGrainTable, grainTableOffset,
									  ba, off+total, fromGrainTable );
					if( log.isDebugEnabled() )
						log.debug( len + " " + actual + " " +
								   left + " " + inGrainTable + " " +
								   fromGrainTable );
					total += fromGrainTable;
					posn += fromGrainTable;
					if( parentStream != null )
						parentStream.skip( fromGrainTable );
				} else if( gt == PARENTGDE ) {
					throw new IllegalStateException( "PARENTGDE!" );
				} else {
					int inGrain = (int)(grainSizeBytes - gOffset );
					int fromGrain = Math.min( left, inGrain );
					if( log.isDebugEnabled() )
						log.debug( len + " " + actual + " " + left + " " +
								   inGrain +
								   " " + fromGrain );
					long gte = gt[gtIndex];
					if( false ) {
					} else if( gte == 0 ) {
						if( log.isDebugEnabled() )
							log.debug( "Zero GT : "+ gdIndex + " " + gtIndex );
						System.arraycopy( zeroGrain, 0,
										  ba, off+total, fromGrain );
						total += fromGrain;
						posn += fromGrain;
						if( parentStream != null )
							parentStream.skip( fromGrain );
					} else if( gte == -1 ) {
						if( parentStream == null )
							throw new IllegalStateException
								( "No parent: " + gdIndex + " " + gtIndex );
						// LOOK: fromParent should ALWAYS == fromGrain
						int fromParent = parentStream.readImpl( ba, off+total,
																fromGrain );
						total += fromParent;
						posn += fromParent;
					} else {
						if( gte != gtePrev ) {
							raf.seek( gte * Constants.SECTORLENGTH );
							GrainMarker gm = GrainMarker.readFrom( raf );
							int nin = raf.read( compressedGrainBuffer,
												0, gm.size );
							if( nin != gm.size )
								throw new IllegalStateException
									( "Partial read: "+ nin + " " + gm.size);
							if( log.isDebugEnabled() ) {
								log.debug( "Inflating " + gdIndex + " "+
										   gtIndex +
										   " = " + nin + " " + gm.lba );
							}
							try {
								int actualLength = uncompressGrain
									( compressedGrainBuffer, 0, nin,
									  grainBuffer );
								if( actualLength != grainSizeBytes )
									throw new IllegalStateException
										( "Bad inflate len: " + actualLength );
							} catch( DataFormatException dfe ) {
								// what now??
								log.warn( dfe );
							}
							gtePrev = gte;
						}
						System.arraycopy( grainBuffer, (int)gOffset,
										  ba, off+total, fromGrain );
						total += fromGrain;
						posn += fromGrain;
						if( parentStream != null )
							parentStream.skip( fromGrain );
					}
				}
				if( log.isDebugEnabled() )
					log.debug( total + " " + posn );
				dPos();
			}
			return total;
		}


		/**
		   Called whenever the local posn changes value.  Do NOT make
		   calls to the parent.dPos here.  Only update parent posn via
		   possible skips() from readImpl above...
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

			// Logically same as posn / grainTableCoverage
			gdIndex = (int)(posn >>> log2GrainTableCoverage);

			/*
			  Logically same as
			  
			  (posn - (gdIndex * grainTableCoverage)) / grainSize
			*/
			long inTable = posn - ((long)gdIndex << log2GrainTableCoverage);
			gtIndex = (int)(inTable >>> log2GrainSize);

			// Logically same as inTable % grainSizeBytes
			gOffset = inTable & (grainSizeBytes - 1);

			if( log.isDebugEnabled() )
				log.debug( "gdIndex: " + gdIndex +
							  " gtIndex: " + gtIndex +
							  " gOffset: " + gOffset );
		}
	
		private final RandomAccessFile raf;
		private final SeekableInputStream parentStream;
		private int log2GrainSize, log2GrainTableCoverage;
		private byte[] compressedGrainBuffer;
		private byte[] grainBuffer;
		private int gdIndex, gtIndex;
		private long gOffset;
		private long gtePrev;
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
		static final int SIZE = 0;

		// Three fields on disk (the 'int size' field is implicit and set to 0)
		static final int FIELDSSIZEOF = 8 + 4 + 4;

		/*
		  When written out to the managed data file, all markers are
		  padded to a sector boundary.  That way, the metadata they
		  describe (grain table, grain directory, etc) starts on its
		  own sector boundary
		*/
		static final int SIZEOF = Constants.SECTORLENGTH;

		static final byte[] PADDING = new byte[SIZEOF-FIELDSSIZEOF];
		
		static final int TYPE_EOS = 0;
		static final int TYPE_GT = 1;
		static final int TYPE_GD = 2;
		static final int TYPE_FOOTER = 3;
	}

	private ManagedDiskDigest parentDigest;
	
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
