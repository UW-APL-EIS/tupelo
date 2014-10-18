package edu.uw.apl.tupelo.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

/**
 * Named after VMWare's own 'stream optimized sparse extent', a way of
 * composing, transmitting and storing virtual machine hard disk
 * (.vmdk) data efficiently, and as used in .ovf files.
 *
 * @see https://www.vmware.com/support/developer/vddk/vmdk_50_technote.pdf
 */

public class StreamOptimizedDisk extends ManagedDisk {

	public StreamOptimizedDisk( PhysicalDisk device, Session session ) {
		this( device, session, GRAINSIZE_DEFAULT );
	}
	
	public StreamOptimizedDisk( PhysicalDisk device, Session session,
								long grainSize ) {
		super( device, null, null );

		// We require the managed data to be a whole number of grains...
		long len = device.size();
		checkSize( len, grainSize );

		String diskID = device.getID();
		UUID parent = Constants.NULLUUID;
		long capacity = len / Constants.SECTORLENGTH;
		header = new Header( diskID, session, DiskTypes.FLAT, parent,
							 capacity, grainSize );

		// dataOffset essentially meaningless for this type of data layout...
		header.dataOffset = 0;
	}
	
		/**
	 * @param diskImage - cannot be a whole disk, e.g. /dev/sda, since
	 * length will be (incorrectly for our purposes) read as 0.  Must
	 * instead be some form of disk image file.  For whole disks, use
	 * PhysicalDisk constructor instead
	 */
	public StreamOptimizedDisk( File diskImage, String diskID,
								Session session ) {
		this( diskImage, diskID, session, GRAINSIZE_DEFAULT );
	}
	
	/**
	 * @param rawData - cannot be a whole disk, e.g. /dev/sda, since
	 * length will be (incorrectly for our purposes) read as 0.  Must
	 * instead be some form of disk image file.
	 */
	public StreamOptimizedDisk( File diskImage, String diskID, Session session,
								long grainSize ) {
		super( null, diskImage, null );

		/*
		  Check that a whole device was not passed in as a File
		  by mistake, e.g. File( "/dev/sda" )
		*/
		long len = diskImage.length();
		if( len == 0 ) {
			throw new IllegalArgumentException
				( "Length 0 for " +	diskImage.getPath() +
				  ". Use PhysicalDevice constructor" );
		}
		checkSize( len, grainSize );

		UUID parent = Constants.NULLUUID;
		long capacity = len / Constants.SECTORLENGTH;
		header = new Header( diskID, session, DiskTypes.FLAT, parent,
							 capacity, grainSize );

		// dataOffset essentially meaningless for this type of data layout...
		header.dataOffset = 0;
	}

	public StreamOptimizedDisk( File managedData, Header h ) {
		super( null, null, managedData );
		header = h;
	}

	private void checkSize( long advertisedSizeBytes, long grainSize ) {
		// We require the managed data to be a whole number of grains...
		long grainSizeBytes = grainSize * Constants.SECTORLENGTH;
		if( advertisedSizeBytes % grainSizeBytes != 0 ) {
			throw new IllegalArgumentException
				( "Data length (" + advertisedSizeBytes +
				  ") must be a multiple of " + grainSizeBytes );
		}
	}
		
	@Override
	public void setParent( ManagedDisk md ) {
		parent = md;
	}

	/*
	  The constructor has already verified that the input data length
	  is a whole number of grains.  We further check (and expect) that
	  it is also a whole number of grains * NUMGTESPERGT.  If so,
	  we can then read in chunks sized to cover an entire grain table.
	  Then, if we find all zeros, we can completely omit that grain
	  table and set that grain directory entry to 0.
	*/
	public void writeTo( OutputStream os ) throws IOException {
		if( device == null && diskImage == null )
			throw new IllegalStateException
				( header.diskID + ": device/diskImage both null" );
		File inFile = device != null ? device.disk : diskImage; 
		FileInputStream fis = new FileInputStream( inFile );
		BufferedInputStream bis = new BufferedInputStream( fis, 1024*1024 );

		DataOutputStream dos = new DataOutputStream( os );
		
		long grainCount = header.capacity / header.grainSize;
		int grainTableCount = (int)alignUp(grainCount, NUMGTESPERGT) /
			NUMGTESPERGT;
		long[] grainDirectory = new long[grainTableCount];
		long[] grainTable = new long[NUMGTESPERGT];
		int gdIndex = 0;
		int gtIndex = 0;
		long lba = 0;

		int wholeGrainTables = (int)(grainCount / NUMGTESPERGT);
		if( wholeGrainTables > 0 ) {
			long grainSizeBytes = header.grainSize * Constants.SECTORLENGTH;
			long grainTableCoverageBytes = grainSizeBytes * NUMGTESPERGT;
			byte[] ba = new byte[(int)grainTableCoverageBytes];
			for( int i = 0; i < wholeGrainTables; i++ ) {
				int nin = bis.read( ba );
				if( nin != ba.length )
					throw new IllegalStateException( "Partial read!" );

				// The whole grain table could be zeros...
				boolean allZeros = true;
				for( int b = 0; b < ba.length; b++ ) {
					if( ba[b] != 0 ) {
						allZeros = false;
						break;
					}
				}
				if( allZeros ) {
					grainDirectory[gdIndex] = 0;
					gdIndex++;
					lba += NUMGTESPERGT * header.grainSize;
					continue;
				}
				
				// Some grains in the table could be zeros...
				for( int g = 0; g < NUMGTESPERGT; g++ ) {
					int offset = grainSizeBytes * g;
					boolean allZeros = true;
					for( int b = 0; b < grainSizeBytes; b++ ) {
						if( ba[offset+b] != 0 ) {
							allZeros = false;
							break;
						}
					}
					if( allZeros ) {
						grainTable[gtIndex] = 0;
						gtIndex++;
						continue;
					}

					// This grain is not zeros, compress
					byte[] compressed = new byte[12];
					// to do
					GrainMarker gm = new GrainMarker
						( lba, compressed.length );
					gm.writeTo( dos );
					// to do
					dos.write( compressed );
					// to do: pad to next sector of os
				}

				/*
				  A table's worth of grains just written, next comes
				  the grain table describing them
				*/
				int fullGrainTableSizeSectors = 4 * NUMGTESPERGT
					/ Constants.SECTORLENGTH;
				MetadataMarker mdm = new MetadataMarker
					( fullGrainTableSizeSectors, MetadataMarker.TYPE_GT );
				mdm.writeTo( dos );
				// to do pad to next sector of os
				for( int i = 0; i < grainTable.length; i++ ) {
					// LOOK: Using 4byte GTE index values restricts us to 2TB
					dos.writeInt( (int)grainTable[i] );
				}
				gtIndex = 0;
				// to do, update gd with sector offset of written GT
			}
		}
			
		int trailingGrains = grainCount - (wholeGrainTables * NUMGTESPERGT);
		if( trailingGrains > 0 ) {
			// to do
		}

		// to do : grain directory
		// to do : footer: redundant header with correct gd offset, which is ?
		// to do: eos marker
	}
	

	public void writeTo( File f ) throws IOException {
		FileOutputStream fos = new FileOutputStream( f );
		BufferedOutputStream bos = new BufferedOutputStream( fos, 1024*1024 );
		writeTo( bos );
		bos.close();
		fos.close();
	}
	
	/**
	 * Example: alignUp( 700, 512 ) -> 1024
	 */
	static long alignUp( long b, int a ) {
		return (long)(Math.ceil( (double)b / a ) * a);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public RandomAccessRead getRandomAccessRead() throws IOException {
		return null;
	}

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
	}

	static class MetadataMarker {
		MetadataMarker( long numSectors, int type ) {
			this.numSectors = numSectors;
			this.type = type;
		}
		void writeTo( DataOutputStream dos ) throws IOException {
			dos.writeLong( numSectors );
			dos.writeInt( type );
		}
		final long numSectors;
		final int type;

		static final int TYPE_GT = 1;
		static final int TYPE_GD = 2;
	}
	
	private ManagedDisk parent;
}

// eof
