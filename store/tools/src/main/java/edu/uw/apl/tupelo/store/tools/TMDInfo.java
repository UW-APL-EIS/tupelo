package edu.uw.apl.tupelo.store.tools;

import java.io.File;

import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;

/**
 * Print a summary of the properties of a .tmd (Tupelo Managed Disk)
 * file supplied in args[0].  Goes straight to the file, 'bypassing'
 * any store logic or layout.
 */
public class TMDInfo {

	static public void main( String[] args ) {
		TMDInfo main = new TMDInfo();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			System.exit(-1);
		} finally {
			LogManager.shutdown();
		}
			  
	}

	public TMDInfo() {
	}

	public void readArgs( String[] args ) {
		Options os = new Options();
		String usage = TMDInfo.class.getName() + " /path/to/tmdfile";
		final String header = "Print properties of given .tmd file.";
		final String footer = "";
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			HelpFormatter hf = new HelpFormatter();
			hf.setWidth( 80 );
			hf.printHelp( usage, header, os, footer );
			System.exit(1);
		}
		args = cl.getArgs();
		if( args.length < 1 ) {
			HelpFormatter hf = new HelpFormatter();
			hf.setWidth( 80 );
			hf.printHelp( usage, header, os, footer );
			System.exit(1);
		}
		inFileName = args[0];
	}
	
	public void start() throws Exception {
		File inFile = new File( inFileName );
		if( !inFile.isFile() ) {
			throw new IllegalStateException
				( inFile + ": No such file" );
		}
		ManagedDisk md = ManagedDisk.readFrom( inFile );
		report( md, inFile );
	}

	public void report( ManagedDisk md, File source ) throws Exception {
		System.out.println( "Source: " + source );
		System.out.println( "Type: " + md.getClass() );
		System.out.println( "Size: " + md.size() );
		System.out.println( "UUID.Create: " + md.getUUIDCreate() );
		System.out.println( "UUID.Parent: " + md.getUUIDParent() );
		ManagedDiskDescriptor mdd = md.getDescriptor();
		System.out.println( "DiskID: " + mdd.getDiskID() );
		System.out.println( "Session: " + mdd.getSession().format() );
	}
	

	String inFileName;
}

// eof
