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

	/**
	 * @param rawData - cannot be a whole disk, e.g. /dev/sda, since
	 * length will be (incorrectly for our purposes) read as 0.  Must
	 * instead be some form of disk image file.
	 */
	public StreamOptimizedDisk( File rawData, String diskID, Session session ) {
		this( rawData, diskID, session,
			  ManagedDisk.GRAINSIZE_DEFAULT );
	}
	
	/**
	 * @param rawData - cannot be a whole disk, e.g. /dev/sda, since
	 * length will be (incorrectly for our purposes) read as 0.  Must
	 * instead be some form of disk image file.
	 */
	public StreamOptimizedDisk( File rawData, String diskID, Session session,
								long grainSize ) {
		super( rawData, null );
		header = new Header( diskID, session, DiskTypes.FLAT, Constants.NULLUUID,
							 rawData.length()/Constants.SECTORLENGTH, grainSize );
		// dataOffset essentially meaningless for this type of data layout...
		header.dataOffset = 0;
	}

	public StreamOptimizedDisk( File managedData, Header h ) {
		super( null, managedData );
		header = h;
	}

	@Override
	public void setParent( ManagedDisk md ) {
		parent = md;
	}

	public void writeTo( OutputStream os ) throws IOException {
		if( rawData == null )
			throw new IllegalStateException( "rawData missing" );

		long grainCount = header.capacity / header.grainSize;
		// LOOK: ceil needed
		int grainTableCount = (int)(grainCount / NUMGTESPERGT);

		long grainTableCoverageBytes =
			header.grainSize * Constants.SECTORLENGTH * NUMGTESPERGT;
		byte[] ba = new byte[(int)grainTableCoverageBytes];
		FileInputStream fis = new FileInputStream( rawData );
		BufferedInputStream bis = new BufferedInputStream( fis, 1024*1024 );

		long[] grainDirectory = new long[grainTableCount];
		long[] grainTable = new long[NUMGTESPERGT];
		int gdIndex = 0;
		int gtIndex = 0;
		while( true ) {
			int nin = bis.read( ba );
			if( nin < 0 )
				break;
			
			/*
			  Case we have a whole grain table worth of data, true
			  until last sectors of disk (and possibly always true)
			*/
			if( nin == ba.length ) {
				boolean allZeros = true;
				for( int b = 0; b < ba.length; b++ ) {
					if( ba[b] != 0 ) {
						allZeros = false;
						break;
					}
				}
				if( allZeros ) {
					grainDirectory[gdIndex] = 0;
				}
			}
				
		}
		
		throw new IllegalStateException( getClass() + "TODO" );
	}

	public void writeTo( File f ) throws IOException {
		FileOutputStream fos = new FileOutputStream( f );
		BufferedOutputStream bos = new BufferedOutputStream( fos, 1024*1024 );
		writeTo( bos );
		bos.close();
		fos.close();
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
	}
	
	private ManagedDisk parent;
}

// eof
