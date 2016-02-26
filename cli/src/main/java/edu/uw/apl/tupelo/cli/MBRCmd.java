package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

/**
 * For each managed disk in a store, attempt to retrieve the 'hashvs'
 * attribute.  If found, locate and print the line containing the
 * sector sequence '0 1', which is typically the Master Boot Record
 * (or GUID?)
 *
 * Changes over time to this sector might uncover malicious activity
 */
  
public class MBRCmd extends Command {
	MBRCmd() {
		super( "mbr",
			   "View hashvs attribute, sector 0 (typically MasterBootRecord)" );
	}
	
	@Override
	public void invoke( String[] args ) throws Exception {
		Options os = commonOptions();
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
			commonParse( cl );
		} catch( ParseException pe ) {
			//	printUsage( os, usage, HEADER, FOOTER );
			//System.exit(1);
		}
		args = cl.getArgs();
		if( args.length < 1 ) {
			System.err.println( "Need store args" );
			return;
		}
		Config c = new Config();
		c.load( config );
		
		String storeName = args[0];
		Config.Store selectedStore = null;
		for( Config.Store cs : c.stores() ) {
			if( cs.getName().equals( storeName ) ) {
				selectedStore = cs;
				break;
			}
		}
		if( selectedStore == null ) {
			System.err.println( "'" + storeName + "' is not a store" );
			return;
		}
		Store store = createStore( selectedStore );	
		
		Collection<ManagedDiskDescriptor> mdds = null;
		try {
			mdds = store.enumerate();
		} catch( ConnectException ce ) {
			System.err.println
				( "Network Error. Is the remote Tupelo store up?" );
			System.exit(0);
		}
		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted,
						  ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		
		for( ManagedDiskDescriptor mdd : sorted ) {
			byte[] value = store.getAttribute( mdd, "hashvs" );
			if( value == null )
				continue;
			try {
				process( value, mdd );
			} catch( IOException ioe ) {
				System.err.println( ioe );
			}
		}
	}

	static void process( byte[] hashvs, ManagedDiskDescriptor mdd )
		throws IOException {
		String s = new String( hashvs );
		StringReader sr = new StringReader( s );
		BufferedReader br = new BufferedReader( sr );
		String line;
		while( (line = br.readLine()) != null ) {
			if( line.startsWith( "0 1 " ) )
				System.out.println( mdd.getDiskID() + " " +
									mdd.getSession() + " " +
									line.substring( 4 ) );
		}
		br.close();
	}
}

// eof
