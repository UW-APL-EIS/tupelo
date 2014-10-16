package edu.uw.apl.tupelo.model;

import java.io.BufferedOutputStream;
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
 * (.vmdk) data efficiently
 */

public class StreamOptimizedDisk extends ManagedDisk {

	/**
	 * @param rawImage - cannot be a whole disk, e.g. /dev/sda, since
	 * length will be (incorrectly for our purposes) read as 0.  Must
	 * instead be some form of disk image file.
	 */
	public StreamOptimizedDisk( File rawData, String diskID, Session session ) {
		this( rawData, diskID, session,
			  ManagedDisk.GRAINSIZE_DEFAULT );
	}
	
	/**
	 * @param rawImage - cannot be a whole disk, e.g. /dev/sda, since
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

	public StreamOptimizedDisk( File managedDisk, Header h ) {
		super( null, managedDisk );
		header = h;
	}

	@Override
	public void setParent( ManagedDisk md ) {
		throw new IllegalStateException( getClass() + "TODO" );
	}

	public void writeTo( OutputStream os ) throws IOException {
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

	private ManagedDisk parent;
}

// eof
