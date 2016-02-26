package edu.uw.apl.tupelo.cli;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.*;

import edu.uw.apl.tupelo.config.Config;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

import edu.uw.apl.tupelo.amqp.objects.FileHashQuery;
import edu.uw.apl.tupelo.amqp.objects.FileHashResponse;
import edu.uw.apl.tupelo.amqp.objects.Utils;

/**
 * The file hashes (assumed to be md5 hashes, LOOK extend to handle
 * e.g. sha1) are read from
 *
 * (a) cmd line arguments.  Use xargs to convert a text file of hashes
 * to cmd line args, e.g. cat FILE | xargs tup search S
 */

public class SearchCmd extends Command {
	SearchCmd() {
		super( "search", "Search a store given file hash (IOCs)" );
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

		log = LogFactory.getLog( SearchCmd.class );

		List<String> hashes = new ArrayList();

		if( args.length > 1 ) {
			// hashes are in cmd line args
			for( int i = 1; i < args.length; i++ ) {
				String arg = args[i];
				arg = arg.trim();
				if( arg.isEmpty() || arg.startsWith( "#" ) )
					continue;
				hashes.add( arg );
			}
		} else {
			// hashes are on stdin
			InputStreamReader isr = new InputStreamReader( System.in );
			BufferedReader br = new BufferedReader( isr );
			String line;
			while( (line = br.readLine()) != null ) {
				line = line.trim();
				if( line.isEmpty() || line.startsWith( "#" ) )
					continue;
				hashes.add( line );
			}
			br.close();
		}
		
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

		FileHashQuery fhq = new FileHashQuery( "md5" );
		for( String hash : hashes ) {
			char[] cs = hash.toCharArray();
			byte[] bs = Hex.decodeHex( cs );
			fhq.add( bs );
		}

		Gson gson = Utils.createGson( true );
		String fhqJ = gson.toJson( fhq );
		System.out.println( fhqJ );
		
		FileHashResponse fhr = new FileHashResponse( fhq.algorithm );

		for( ManagedDiskDescriptor mdd : sorted ) {
			System.out.println( "Search " + mdd );
			log.info( "Search " + mdd );
			List<String> ss = loadFileHashes( mdd, store );
			if( false && ss.size() > 0 )
				System.out.println( "Loaded " + ss.size() );
			
			log.info( "Loaded " + ss.size() + " hashes from " + mdd );
			/*
			  Recall: The file content of MANY paths can hash to
			  the SAME value, typically when the file is empty.
			*/
			Map<BigInteger,List<String>> haystack = buildHaystack( ss );
			
			for( byte[] hash : fhq.hashes ) {
				// 1 means 'this value is positive'
				BigInteger needle = new BigInteger( 1, hash );
				List<String> paths = haystack.get( needle );
				if( paths == null )
					continue;
				String hashHex = new String( Hex.encodeHex( hash ) );
				log.info( "Matched " +  hashHex + ": " + mdd + " "
						  + paths );
				for( String path : paths )
					fhr.add( hash, mdd, path );
			}					
		}

		String fhrJ = gson.toJson( fhr );
		System.out.println( fhrJ );

	}
	
	/*
	  Load the 'md5' attribute data from the store for the given
	  ManagedDiskDescriptor.  Just load the whole lines, do NOT parse
	  anything at this point.
	*/
	private List<String> loadFileHashes( ManagedDiskDescriptor mdd,
										 Store store )
		throws IOException {
		List<String> result = new ArrayList<String>();
		Collection<String> attrNames = store.listAttributes( mdd );
		for( String attrName : attrNames ) {
			if( !attrName.startsWith( "hashfs-" ) )
				continue;
			byte[] ba = store.getAttribute( mdd, attrName );
			if( ba == null )
				continue;
			ByteArrayInputStream bais = new ByteArrayInputStream( ba );
			InputStreamReader isr = new InputStreamReader( bais );
			LineNumberReader lnr = new LineNumberReader( isr );
			String line = null;
			while( (line = lnr.readLine()) != null ) {
				line = line.trim();
				if( line.isEmpty() || line.startsWith( "#" ) )
					continue;
				result.add( line );
			}
		}
		return result;
	}


	/**
	 * Turn the contents of a manageddisk's md5 attribute, stored as a
	 * text file of the form
	 *
	 * hashHex /path/to/file
	 * hashHex /path/to/other/file
	 *
	 * into a map of hash (as BigInteger) -> List<String>
	 *
	 * Then, given an hash as needle, we can locate ALL files with that hash
	 */
	private Map<BigInteger,List<String>> buildHaystack( List<String> ss ) {
		Map<BigInteger,List<String>> result =
			new HashMap<BigInteger,List<String>>();
		for( String s : ss ) {
			String[] toks = s.split( "\\s+", 2 );
			if( toks.length < 2 ) {
				log.warn( s );
				continue;
			}
			String md5Hex = toks[0];
			String path = toks[1];
			byte[] md5 = null;
			try {
				md5 = Hex.decodeHex( md5Hex.toCharArray() );
			} catch( DecoderException de ) {
				log.warn( de );
				continue;
			}
			// 1 means 'this value is positive'
			BigInteger bi = new BigInteger( 1, md5 );
			List<String> paths = result.get( bi );
			if( paths == null ) {
				paths = new ArrayList<String>();
				result.put( bi, paths );
			}
			paths.add( path );
		}
		return result;
	}

	Log log;
}

// eof
