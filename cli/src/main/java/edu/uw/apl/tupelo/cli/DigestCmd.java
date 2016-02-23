package edu.uw.apl.tupelo.cli;

import java.io.Console;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;
import edu.uw.apl.tupelo.store.null_.NullStore;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;

public class DigestCmd extends Command {
	DigestCmd() {
		super( "digest", "Compute md5 hash for store-managed disks" );
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
		Store s = createStore( selectedStore );
		FilesystemStore fs = (FilesystemStore)s;
		Collection<ManagedDiskDescriptor> mdds = fs.enumerate();

		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted,
						  ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		for( ManagedDiskDescriptor mdd : sorted ) {
			long sz = fs.size( mdd );
			boolean proceed = proceedTest( mdd );
			if( !proceed )
				continue;
			System.out.println( "Digesting: " + mdd +
								" (" + sz + " bytes)" );
			fs.computeDigest( mdd );
		}
	}

	private boolean proceedTest( ManagedDiskDescriptor mdd ) {
		Console c = System.console();
		if( c == null )
			return true;
		String line = c.readLine( "" + mdd + "? y/n " );
		if( line == null )
			return false;
		line = line.trim();
		if( line.isEmpty() )
			return false;
		return line.charAt(0) == 'y' || line.charAt(0) == 'Y';
	}
}

// eof
