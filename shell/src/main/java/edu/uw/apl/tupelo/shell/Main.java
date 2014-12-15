package edu.uw.apl.tupelo.shell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Properties;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.net.URI;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

/*
  import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
*/
//import com.google.gson.Gson;

import org.apache.commons.cli.*;

import edu.uw.apl.commons.shell.Shell;

import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.PhysicalDisk;
import edu.uw.apl.tupelo.model.VirtualDisk;
import edu.uw.apl.tupelo.model.UnmanagedDisk;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.null_.NullStore;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;

/**
   A cmd line shell for the Tupelo system. Works along the lines of
   bash...
*/

public class Main extends Shell {

    static public void main( String[] args ) {
		try {
			Main main = new Main();
			main.readConfig();
			main.readArgs( args );
			main.start();
			main.finish();
		} catch( Exception e ) {
			if( debug )
				e.printStackTrace();
			else
				System.err.println( e );
		}
	}

	public Main() {
		super();
		physicalDisks = new ArrayList<PhysicalDisk>();
		virtualDisks = new ArrayList<VirtualDisk>();
		diskImages = new ArrayList<DiskImage>();
		
		String tmpDirS = System.getProperty( "java.io.tmpdir" );
		tmpDir = new File( tmpDirS );
		storeLocation = "./test-store";
		verbose = true;

		// report available Java memory...
		addCommand( "mem", new Lambda() {
				public void apply( String[] args ) throws Exception {
					Runtime rt = Runtime.getRuntime();
					long mem = rt.freeMemory();
					System.out.println( "Free Memory: " + mem );
				}
			} );
		commandHelp( "mem", "Print free memory" );
					
		// report store-managed data...
		addCommand( "ms", new Lambda() {
				public void apply( String[] args ) throws Exception {
					Collection<ManagedDiskDescriptor> mdds = store.enumerate();
					reportManagedDisks( mdds );
				}
			} );
		commandHelp( "ms", "List store-managed disks" );

		// report store's free disk space...
		addCommand( "space", new Lambda() {
				public void apply( String[] args ) throws Exception {
					long usableSpace = store.getUsableSpace();
					System.out.println( "Usable space: " + usableSpace );
										 
				}
			} );
		commandHelp( "space", "Print store's free disk space" );

		// report unmanaged data...
		addCommand( "us", new Lambda() {
				public void apply( String[] args ) throws Exception {
					reportUnmanagedDisks();
										 
				}
			} );
		commandHelp( "space", "Print store's free disk space" );

		// putData, xfer an unmanaged disk to the store
		addCommand( "putdisk", "(.+)", new Lambda() {
				public void apply( String[] args ) throws Exception {
					String udName = args[1];
					UnmanagedDisk ud = locateUnmanagedDisk( udName );
					if( ud == null ) {
						System.out.println( "No unmanaged disk: " +
											udName );
						return;
					}
					try {
						putDisk( ud );
					} catch( IOException ioe ) {
						log.warn( ioe );
						System.err.println( "" + ioe );
					}
				}
			} );
		commandHelp( "putdisk", "unmanagedDisk",
					 "Transfer an identified unmanaged disk to the store" );
	}

	public void readArgs( String[] args ) throws Exception {
		Options os = new Options();
		os.addOption( "c", true, "command string" );
		os.addOption( "d", false, "debug" );
		os.addOption( "s", true, "store location" );
		os.addOption( "u", true, "unmanaged disk" );
		CommandLineParser clp = new PosixParser();
		CommandLine cl = clp.parse( os, args );
		debug = cl.hasOption( "d" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
		if( cl.hasOption( "c" ) ) {
			cmdString = cl.getOptionValue( "c" );
		}
		if( cl.hasOption( "u" ) ) {
			String[] ss = cl.getOptionValues( "u" );
			for( String s : ss ) {
				File f = new File( s );
				if( !( f.isFile() && f.canRead() ) )
					continue;
				if( VirtualDisk.likelyVirtualDisk( f ) ) {
					VirtualDisk vd = new VirtualDisk( f );
					virtualDisks.add( vd );
				} else {
					DiskImage di = new DiskImage( f );
					diskImages.add( di );
				}
			}
		}
		args = cl.getArgs();
		if( args.length > 0 ) {
			cmdFile = new File( args[0] );
			if( !cmdFile.exists() ) {
				// like bash would do, write to stderr...
				System.err.println( cmdFile + ": No such file or directory" );
				System.exit(-1);
			}
		}
	}

	@Override
	public void start() throws Exception {
		store = buildStore();
		report( "Store: " + store );
		identifyUnmanagedDisks();
		super.start();
	}

	private UnmanagedDisk locateUnmanagedDisk( String needle ) {
		for( PhysicalDisk pd : physicalDisks ) {
			String s = pd.getSource().getPath();
			if( s.startsWith( needle ) )
				return pd;
		}
		for( VirtualDisk vd : virtualDisks ) {
			String s = vd.getSource().getName();
			if( s.startsWith( needle ) )
				return vd;
		}
		for( DiskImage di : diskImages ) {
			String s = di.getSource().getName();
			if( s.startsWith( needle ) )
				return di;
		}
		return null;
	}
	
	private void identifyUnmanagedDisks() {
		identifyPhysicalDisks();
	}

	private void identifyPhysicalDisks() {
		for( String pdName : PHYSICALDISKNAMES ) {
			File pdf = new File( pdName );
			if( !pdf.exists() )
				continue;
			if( !pdf.canRead() ) {
				if( isInteractive() ) {
					System.out.println( "Unreadable: " + pdf );
					continue;
				}
			}	
			try {
				PhysicalDisk pd = new PhysicalDisk( pdf );
				log.info( "Located " + pdf );
				physicalDisks.add( pd );
			} catch( IOException ioe ) {
				log.error( ioe );
			}
		}
	}
	
	private void finish() throws Exception {
		if( isInteractive() )
			System.out.println( "Bye!" );
	}

	private void report( String msg ) {
		if( !isInteractive() )
			return;
		System.out.println( msg );
	}
	
	Store buildStore() {
		Store s = null;
		if( false ) {
		} else if( storeLocation.equals( "/dev/null" ) ) {
			s = new NullStore();
		} else if( storeLocation.startsWith( "http" ) ) {
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

	@Override
	protected void prompt() {
		System.out.print( "tupelo> " );
		System.out.flush();
	}
	
	void reportManagedDisks( Collection<ManagedDiskDescriptor> mdds ) {
		String header = String.format( MANAGEDDISKREPORTFORMAT,
									   "ID", "Session" );
		System.out.println( header );
		for( ManagedDiskDescriptor mdd : mdds ) {
			String fmt = String.format( MANAGEDDISKREPORTFORMAT,
										mdd.getDiskID(),
										mdd.getSession() );
			System.out.println( fmt );
		}
	}

	void reportUnmanagedDisks() {
		String header = String.format( UNMANAGEDDISKREPORTFORMAT,
									   "ID", "Size", "Path" );
		System.out.println( header );
		reportPhysicalDisks();
		reportVirtualDisks();
	}

	void reportPhysicalDisks() {
		for( PhysicalDisk pd : physicalDisks ) {
			String fmt = String.format( UNMANAGEDDISKREPORTFORMAT,
										pd.getID(), pd.size(), pd.getSource() );
			System.out.println( fmt );
		}
	}

	void reportVirtualDisks() {
		for( VirtualDisk vd : virtualDisks ) {
			String fmt = String.format( UNMANAGEDDISKREPORTFORMAT,
										vd.getID(), vd.size(),
										vd.getSource().getName() );
			System.out.println( fmt );
		}
	}

	private void putDisk( UnmanagedDisk ud ) throws IOException {
		Session session = store.newSession();
		boolean useFlatDisk = ud.size() < 1024L * 1024 * 1024;
		
	}
	
	String storeLocation;
	Store store;
	List<PhysicalDisk> physicalDisks;
	List<VirtualDisk> virtualDisks;
	List<DiskImage> diskImages;
	File tmpDir;
	static boolean verbose, debug;

	static public final String[] PHYSICALDISKNAMES = {
		// Linux/Unix...
		"/dev/sda", "/dev/sdb", "/dev/sdc",
		"/dev/sdd", "/dev/sde", "/dev/sdf",
		// MacOS...
		"/dev/disk0", "/dev/disk1", "/dev/disk2"
	};

	static final String UNMANAGEDDISKREPORTFORMAT = "%42s %16s %16s";

	static final String MANAGEDDISKREPORTFORMAT = "%42s %17s";
}


// eof
