package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
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

public class InfoCmd extends Command {
	InfoCmd() {
		super( "info", "Print info on store content" );
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
		
		System.out.println();
		System.out.printf( "%-16s: %s\n",
						   "Store Location", selectedStore.getUrl() );
		System.out.printf( "%-16s: %s\n",
						   "Usable Space", store.getUsableSpace() );
		System.out.printf( "%-16s: %s\n",
							"UUID", store.getUUID() );
		System.out.println();
		
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
		
		System.out.println( "Contents:" );
		System.out.println();
		int i = 1;
		for( ManagedDiskDescriptor mdd : sorted ) {
			report( mdd, store, i );
			i++;
		}
	}

	private void report( ManagedDiskDescriptor mdd, Store store, int n )
		throws IOException {
		System.out.println( n + " " + mdd.getDiskID() + ", " +
							mdd.getSession() );
		System.out.println( " Size: " + store.size( mdd ) );
		Collection<String> attrNames = store.listAttributes( mdd );
		System.out.println( " Attributes: " + attrNames );
	}
}

// eof
