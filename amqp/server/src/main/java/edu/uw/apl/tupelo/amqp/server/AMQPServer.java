package edu.uw.apl.tupelo.amqp.server;

import java.io.File;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;
import edu.uw.apl.tupelo.utils.Discovery;

/**
 * A 'main' front end around the primary logic in {@link
 * FileHashService}.  Does cmd line processing to extract from the
 * user (and by defaults) values for a Tupelo store location and a
 * RabbitMQ broker location.
 *
 * LOOK: Currently we know about a single store only.  Extend to
 * many??
 */

public class AMQPServer {

	static public void main( String[] args ) throws Exception {
		AMQPServer main = new AMQPServer();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			if( debug )
				e.printStackTrace();
			System.exit(-1);
		}
	}

	AMQPServer() {
		log = Logger.getLogger( getClass() );
		storeLocation = STORELOCATIONDEFAULT;
		brokerUrl = Discovery.locatePropertyValue( "amqp.url" );
	}


	public void readArgs( String[] args ) {
		Options os = new Options();
		os.addOption( "d", false, "Debug" );
		os.addOption( "v", false, "Verbose" );
		os.addOption( "s", true,
					  "Store url/directory. Defaults to " +
					  STORELOCATIONDEFAULT );
		os.addOption( "u", true,
					  "Broker url. Can also be located on path and in resource" );

		final String USAGE =
			AMQPServer.class.getName() +
			" [-d] [-v] [-s storeLocation] [-u brokerURL]";
		final String HEADER = "";
		final String FOOTER = "";
		
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		debug = cl.hasOption( "d" );
		verbose = cl.hasOption( "v" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
		if( cl.hasOption( "u" ) ) {
			brokerUrl = cl.getOptionValue( "u" );
		}
		args = cl.getArgs();
	}

	public void start() throws Exception {
		store = buildStore( storeLocation );

		// Print some info about the store state..
		System.out.println( "Store location : " + storeLocation );
		UUID id = store.getUUID();
		System.out.println( "Store id : " + id );
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted, ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		System.out.println( "Store managed disks: " );
		for( ManagedDiskDescriptor mdd : sorted )
			System.out.println( mdd );

		// Print some info about the broker..
		System.out.println( "AMQP broker : " + brokerUrl );

		// And start the main service...
		FileHashService fhs = new FileHashService( store, brokerUrl );
		fhs.start();
	}
	
	static public Store buildStore( String storeLocation ) {
		Store s = null;
		if( storeLocation.startsWith( "http" ) ) {
			s = new HttpStoreProxy( storeLocation );
		} else {
			File dir = new File( storeLocation );
			if( !dir.isDirectory() ) {
				throw new IllegalStateException
					( "Not a directory: " + storeLocation );
			}
			s = new FilesystemStore( dir );
		}
		return s;
	}
	
	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	private String storeLocation;
	private Store store;
	private String brokerUrl;
	private Logger log;
	
	static boolean debug, verbose;
	
	static final String STORELOCATIONDEFAULT = "./test-store";

}

// eof
