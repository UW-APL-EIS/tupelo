package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class CatCmd extends Command {
	CatCmd() {
		super( "cat", "Cat a store-managed disk" );
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
		if( args.length < 2 ) {
			System.err.println( "Need store arg + index" );
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

		int index = Integer.parseInt( args[1] );

		Collection<ManagedDiskDescriptor> mdds = null;
		try {
			mdds = store.enumerate();
		} catch( ConnectException ce ) {
			System.err.println
				( "Network Error. Is the remote Tupelo store up?" );
			System.exit(0);
		}
		if( index < 1 || index > mdds.size() ) {
			System.err.println( "Index out-of-range" );
			return;
		}

		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted,
						  ManagedDiskDescriptor.DEFAULTCOMPARATOR );

		ManagedDiskDescriptor mdd = sorted.get(index-1);
		ManagedDisk md = store.locate( mdd );
		InputStream is = md.getInputStream();
		byte[] ba = new byte[1024*4];
		while( true ) {
			int nin = is.read( ba );
			if( nin < 1 )
				break;
			System.out.write( ba, 0, nin );
		}
		
	}
}

// eof
