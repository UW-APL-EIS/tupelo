package edu.uw.apl.tupelo.shell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import edu.uw.apl.commons.sleuthkit.image.Image;
import edu.uw.apl.commons.sleuthkit.filesys.FileSystem;
import edu.uw.apl.commons.sleuthkit.volsys.Partition;
import edu.uw.apl.commons.sleuthkit.volsys.VolumeSystem;
import edu.uw.apl.commons.sleuthkit.digests.BodyFile;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileBuilder;
import edu.uw.apl.commons.sleuthkit.digests.BodyFileCodec;
import edu.uw.apl.commons.sleuthkit.digests.VolumeSystemHash;
import edu.uw.apl.commons.sleuthkit.digests.VolumeSystemHashCodec;

import edu.uw.apl.vmvols.model.VirtualMachine;
import edu.uw.apl.vmvols.fuse.VirtualMachineFileSystem;


import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.ManagedDiskDigest;
import edu.uw.apl.tupelo.model.DiskImage;
import edu.uw.apl.tupelo.model.FlatDisk;
import edu.uw.apl.tupelo.model.PhysicalDisk;
import edu.uw.apl.tupelo.model.VirtualDisk;
import edu.uw.apl.tupelo.model.ProgressMonitor;
import edu.uw.apl.tupelo.model.StreamOptimizedDisk;
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
		vmNames = new HashSet<String>();
		
		String tmpDirS = System.getProperty( "java.io.tmpdir" );
		tmpDir = new File( tmpDirS );
		storeLocation = "./test-store";
		verbose = false;

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
		commandHelp( "us", "List local unmanaged disks" );

		// report the store url...
		addCommand( "s", new Lambda() {
				public void apply( String[] args ) throws Exception {
					System.out.println( "Store: " + storeLocation );
					
										 
				}
			} );
		commandHelp( "s", "Print the store location" );

		/*
		  vshash, hash the volume system (unallocated areas) of an identified
		  unmanaged disk
		*/
		addCommand( "vshash", "(.+)", new Lambda() {
				public void apply( String[] args ) throws Exception {
					String needle = args[1];
					needle = needle.trim();
					UnmanagedDisk ud = null;
					try {
						ud = locateUnmanagedDisk( needle );
					} catch( IndexOutOfBoundsException oob ) {
						System.err.println
							( "No unmanaged disk " + needle +
							  ". Use 'us' to list unmanaged disks" );
						return;
					}
					if( ud == null ) {
						System.out.println( "No unmanaged disk: " +
											needle );
						return;
					}
					hashVolumeSystem( ud );
				}
			} );
		commandHelp( "vshash", "unmanagedDisk",
					 "Hash each unallocated area of the identified unmanaged disk, storing the result as a managed disk attribute" );

		/*
		  hashfs, hash any/all filesystems of an identified
		  unmanaged disk
		*/
		addCommand( "fshash", "(.+)", new Lambda() {
				public void apply( String[] args ) throws Exception {
					String needle = args[1];
					needle = needle.trim();
					UnmanagedDisk ud = null;
					try {
						ud = locateUnmanagedDisk( needle );
					} catch( IndexOutOfBoundsException oob ) {
						System.err.println
							( "No unmanaged disk " + needle +
							  ". Use 'us' to list unmanaged disks" );
						return;
					}
					if( ud == null ) {
						System.out.println( "No unmanaged disk: " +
											needle );
						return;
					}
					hashFileSystems( ud );
				}
			} );
		commandHelp( "fshash", "unmanagedDisk",
					 "Hash each filesystem of the identified unmanaged disk.  The resulting bodyfile is stored as a managed disk attribute" );
		
		// putdisk, xfer an unmanaged disk to the store
		addCommand( "putdisk", "(.+)", new Lambda() {
				public void apply( String[] args ) throws Exception {
					String needle = args[1];
					UnmanagedDisk ud = null;
					try {
						ud = locateUnmanagedDisk( needle );
					} catch( IndexOutOfBoundsException oob ) {
						System.err.println
							( "No unmanaged disk " + needle +
							  ". Use 'us' to list unmanaged disks" );
						return;
					}
					if( ud == null ) {
						System.out.println( "No unmanaged disk: " +
											needle );
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
		commandHelp( "putdisk", "unmanagedDiskPath|unmanagedDiskIndex",
					 "Transfer an identified unmanaged disk to the store" );

		// list attributes for a given managed disk
		addCommand( "as", "(.+)", new Lambda() {
				public void apply( String[] args ) throws Exception {
					String needle = args[1];
					ManagedDiskDescriptor mdd = null;
					try {
						mdd = locateManagedDisk( needle );
					} catch( RuntimeException iae ) {
						System.err.println( iae );
						return;
					}
					if( mdd == null ) {
						System.out.println( "No managed disk: " +
											needle );
						return;
					}
					try {
						listAttributes( mdd );
					} catch( IOException ioe ) {
						log.warn( ioe );
						System.err.println( "" + ioe );
					}
				}
			} );
		commandHelp( "as", "managedDiskIndex",
					 "List attributes of an identified managed disk" );
	}

	static protected void printUsage( Options os, String usage,
									  String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}


	public void readArgs( String[] args ) throws Exception {
		Options os = new Options();
		os.addOption( "c", true, "command string" );
		os.addOption( "d", false, "debug" );
		os.addOption( "s", true, "store location (file/http)" );
		os.addOption( "u", true, "path to unmanaged disk" );
		os.addOption( "V", false, "show version number and exit" );

		String USAGE = Main.class.getName() + " [-c] [-d] [-s] [-u] [-V]";
		final String HEADER = "";
		final String FOOTER = "";

		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( Exception e ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		
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
				if( !( f.canRead() ) )
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
		if( cl.hasOption( "V" ) ) {
			Package p = getClass().getPackage();
			String version = p.getImplementationVersion();
			System.out.println( p.getName() + "/" + version );
			System.exit(0);
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
		try {
			store = buildStore();
		} catch( IllegalStateException ise ) {
			System.err.println( "Cannot locate store: '" + storeLocation +
								"'." );
			System.err.println( "For a file-based store, first 'mkdir " +
								storeLocation + "'" );
			System.exit(1);
			
		}
		report( "Store: " + storeLocation );
		identifyUnmanagedDisks();
		super.start();
	}
	
	
	private void listAttributes( ManagedDiskDescriptor mdd ) throws IOException{
		Collection<String> attrNames = store.listAttributes( mdd );
		reportAttributes( attrNames );
	}
	
	private UnmanagedDisk locateUnmanagedDisk( String needle ) {
		try {
			int i = Integer.parseInt( needle );
			return locateUnmanagedDisk( i );
		} catch( NumberFormatException nfe ) {
			// proceed with name-based lookup...
		}
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

	/**
	 * 1-based search, natural numbers
	 */
	private UnmanagedDisk locateUnmanagedDisk( int needle ) {
		int total = physicalDisks.size() + virtualDisks.size() +
			diskImages.size();
		if( needle < 1 || needle > total )
			throw new IndexOutOfBoundsException( "Index out-of-range: " +
												 needle );
		needle--;
		if( needle < physicalDisks.size() )
			return physicalDisks.get( needle );
		needle -= physicalDisks.size();
		if( needle < virtualDisks.size() )
			return virtualDisks.get( needle );
		needle -= virtualDisks.size();
		if( needle < diskImages.size() )
			return diskImages.get( needle );
		// should never occur given earlier bounds check
		return null;
	}
	
	private ManagedDiskDescriptor locateManagedDisk( String needle )
		throws IOException {
		try {
			int i = Integer.parseInt( needle );
			return locateManagedDisk( i );
		} catch( NumberFormatException nfe ) {
			// proceed with name-based lookup...
		}
		// TODO
		return null;
	}

	private ManagedDiskDescriptor locateManagedDisk( int index ) 
		throws IOException {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted, ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		int total = sorted.size();
		if( index < 1 || index > total )
			throw new IllegalArgumentException( "Index out-of-range: " +
												index );
		return sorted.get(index-1);
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
									   "N", "ID", "Session" );
		System.out.println( header );
		int n = 1;
		List<ManagedDiskDescriptor> sorted =
			new ArrayList<ManagedDiskDescriptor>( mdds );
		Collections.sort( sorted, ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		for( ManagedDiskDescriptor mdd : sorted ) {
			String fmt = String.format( MANAGEDDISKREPORTFORMAT, n,
										mdd.getDiskID(),
										mdd.getSession() );
			System.out.println( fmt );
			n++;
		}
	}

	void reportAttributes( Collection<String> attrNames ) {
		String header = String.format( ATTRIBUTEREPORTFORMAT,
									   "N", "Name" );
		System.out.println( header );
		int n = 1;
		List<String> sorted = new ArrayList<String>( attrNames );
		Collections.sort( sorted );
		for( String s : sorted ) {
			String fmt = String.format( ATTRIBUTEREPORTFORMAT, n, s );
			System.out.println( fmt );
			n++;
		}
	}

	void reportUnmanagedDisks() {
		String header = String.format( UNMANAGEDDISKREPORTFORMAT,
									   "N", "ID", "Size", "Path" );
		System.out.println( header );
		reportPhysicalDisks( 1 );
		reportVirtualDisks( 1 + physicalDisks.size() );
		reportDiskImages( 1 + physicalDisks.size() + virtualDisks.size() );
	}

	void reportPhysicalDisks( int n ) {
		for( PhysicalDisk pd : physicalDisks ) {
			String fmt = String.format( UNMANAGEDDISKREPORTFORMAT,
										n, pd.getID(), pd.size(),
										pd.getSource() );
			System.out.println( fmt );
			n++;
		}
	}

	void reportVirtualDisks( int n ) {
		for( VirtualDisk vd : virtualDisks ) {
			String fmt = String.format( UNMANAGEDDISKREPORTFORMAT,
										n, vd.getID(), vd.size(),
										vd.getSource().getName() );
			System.out.println( fmt );
			n++;
		}
	}

	void reportDiskImages( int n ) {
		for( DiskImage di : diskImages ) {
			String fmt = String.format( UNMANAGEDDISKREPORTFORMAT,
										n, di.getID(), di.size(),
										di.getSource().getName() );
			System.out.println( fmt );
			n++;
		}
	}

	private void checkSession() throws IOException {
		if( session == null ) {
			session = store.newSession();
			report( "Session: " + session );
		}
	}
	
	private void putDisk( UnmanagedDisk ud ) throws IOException {
		checkSession();
		Collection<ManagedDiskDescriptor> existing = store.enumerate();
		if( verbose )
			System.out.println( "Stored data: " + existing );

		List<ManagedDiskDescriptor> matching =
			new ArrayList<ManagedDiskDescriptor>();
		for( ManagedDiskDescriptor mdd : existing ) {
			if( mdd.getDiskID().equals( ud.getID() ) ) {
				matching.add( mdd );
			}
		}
		Collections.sort( matching, ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		System.out.println( "Matching managed disks:" );
		for( ManagedDiskDescriptor el : matching ) {
			System.out.println( " " + el.getSession() );
		}

		ManagedDiskDigest digest = null;
		UUID uuid = null;
		if( !matching.isEmpty() ) {
			ManagedDiskDescriptor recent = matching.get( matching.size()-1 );
			log.info( "Retrieving uuid for: "+ recent );
			uuid = store.uuid( recent );
			if( debug )
				System.out.println( "UUID: " + uuid );
			log.info( "Requesting digest for: "+ recent );
			digest = store.digest( recent );
			if( digest == null ) {
				log.warn( "No digest, continuing with full disk put" );
			} else {
				log.info( "Retrieved digest for " +
						  recent.getSession() + ": " +
						  digest.size() );
			}
			
		}
		checkSession();
		ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
															   session );
		System.out.println( "Storing: " + ud.getSource() +
							" (" + ud.size() + " bytes) to " + mdd );
		ManagedDisk md = null;
		boolean useFlatDisk = ud.size() < 1024L * 1024 * 1024;
		if( useFlatDisk ) {
			md = new FlatDisk( ud, session );
		} else {
			if( uuid != null )
				md = new StreamOptimizedDisk( ud, session, uuid );
			else
				md = new StreamOptimizedDisk( ud, session );
			md.setCompression( ManagedDisk.Compressions.SNAPPY );
		}

		if( digest != null )
			md.setParentDigest( digest );
		
		if( !isInteractive() ) {
			store.put( md );
		} else {
			final long sz = ud.size();
			ProgressMonitor.Callback cb = new ProgressMonitor.Callback() {
					@Override
					public void update( long in, long out, long elapsed ) {
						double pc = in / (double)sz * 100;
						System.out.print( (int)pc + "% " );
						System.out.flush();
						if( in == sz ) {
							System.out.println();
							System.out.printf( "Unmanaged size: %12d\n",
											   sz );
							System.out.printf( "Managed   size: %12d\n", out );
							System.out.println( "Elapsed: " + elapsed );
						}
					}
				};
			int progressMonitorUpdateIntervalSecs = 5;
			store.put( md, cb, progressMonitorUpdateIntervalSecs );
		}

		/*
		  Store various attributes about the UNMANAGED state that
		  serve as a context for the acquisition.  None of these are
		  REQUIRED to be 'correct', they are purely informational.
		*/
		String user = System.getProperty( "user.name" );
		store.setAttribute( mdd, "unmanaged.user",
							user.getBytes() );
		Date timestamp = new Date();
		// storing the timestamp as a STRING
		store.setAttribute( mdd, "unmanaged.timestamp",
							("" + timestamp).getBytes() );
		
		store.setAttribute( mdd, "unmanaged.path",
							ud.getSource().getPath().getBytes() );

		try {
			InetAddress ia = InetAddress.getLocalHost();
			String dotDecimal = ia.getHostAddress();
			store.setAttribute( mdd, "unmanaged.inetaddress",
								dotDecimal.getBytes() );
		} catch( UnknownHostException uhe ) {
		}
			
	}

	private void hashVolumeSystem( UnmanagedDisk ud ) throws IOException {
		checkSession();
		if( ud instanceof VirtualDisk ) {
			hashVolumeSystemVirtual( (VirtualDisk)ud );
		} else {
			hashVolumeSystemNonVirtual( ud );
		}
	}

		
	private void checkVMFS() throws IOException {
		if( vmfs == null ) {
			vmfs = new VirtualMachineFileSystem();
			final VirtualMachineFileSystem vmfsF = vmfs;
			Runtime.getRuntime().addShutdownHook( new Thread() {
				public void run() {
					try {
						vmfsF.umount();
					} catch( Exception e ) {
						System.err.println( e );
					}
				}
				} );
			final File mountPoint = new File( "vmfs" );
			mountPoint.mkdirs();
			mountPoint.deleteOnExit();
			try {
				vmfs.mount( mountPoint, true );
			} catch( Exception e ) {
				throw new IOException( e );
			}
		}
	}
	
	private void hashVolumeSystemVirtual( VirtualDisk ud ) throws IOException {
		checkVMFS();
		VirtualMachine vm = ud.getVM();
		vmfs.add( vm );
		edu.uw.apl.vmvols.model.VirtualDisk vmDisk = ud.getDelegate();
		File f = vmfs.pathTo( vmDisk );
		Image i = new Image( f );
		try {
			hashVolumeSystemImpl( i, ud );
		} finally {
			// MUST release i else leaves vmfs non-unmountable
			i.close();
		}

	}
	
	private void hashVolumeSystemNonVirtual( UnmanagedDisk ud )
		throws IOException {
		Image i = new Image( ud.getSource() );
		try {
			hashVolumeSystemImpl( i, ud );
		} finally {
			i.close();
		}
	}


	private void hashVolumeSystemImpl( Image i, UnmanagedDisk ud )
		throws IOException {
		VolumeSystem vs = null;
		try {
			vs = new VolumeSystem( i );
		} catch( IllegalStateException noVolSys ) {
			log.warn( noVolSys );
			return;
		}
		try {
			VolumeSystemHash vsh = VolumeSystemHash.create( vs );
			StringWriter sw = new StringWriter();
			VolumeSystemHashCodec.writeTo( vsh, sw );
			String s = sw.toString();
			ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
																   session );
			String key = "hashvs";
			byte[] value = s.getBytes();
			store.setAttribute( mdd, key, value );
		} finally {
			// MUST release vs else leaves vmfs non-unmountable
			vs.close();
		}
	}
	
	private void hashFileSystems( UnmanagedDisk ud ) throws IOException {
		checkSession();
		if( ud instanceof VirtualDisk ) {
			hashFileSystemsVirtual( (VirtualDisk)ud );
		} else {
			hashFileSystemsNonVirtual( ud );
		}
	}

	private void hashFileSystemsVirtual( VirtualDisk ud ) throws IOException {
		checkVMFS();
		VirtualMachine vm = ud.getVM();
		if( !vmNames.contains( vm.getName() ) ) {
			vmfs.add( vm );
			vmNames.add( vm.getName() );
		}
		edu.uw.apl.vmvols.model.VirtualDisk vmDisk = ud.getDelegate();
		File f = vmfs.pathTo( vmDisk );
		Image i = new Image( f );
		try {
			hashFileSystemsImpl( i, ud );
		} finally {
			// MUST release i else leaves vmfs non-unmountable
			i.close();
		}
	}

	private void hashFileSystemsNonVirtual( UnmanagedDisk ud )
		throws IOException {
		Image i = new Image( ud.getSource() );
		try {
			hashFileSystemsImpl( i, ud );
		} finally {
			// MUST release i else leaves vmfs non-unmountable
			i.close();
		}
	}
	
	private void hashFileSystemsImpl( Image i, UnmanagedDisk ud )
		throws IOException {
		VolumeSystem vs = null;
		try {
			vs = new VolumeSystem( i );
		} catch( IllegalStateException noVolSys ) {
			log.warn( noVolSys );
			return;
		}
		try {
			ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( ud.getID(),
																   session );
			List<Partition> ps = vs.getPartitions();
			for( Partition p : ps ) {
				log.debug( p.start() + " " + p.length() + " " +
						   p.description() );
				if( !p.isAllocated() )
					continue;
				FileSystem fs = null;
				try {
					fs = new FileSystem( i, p.start() );
				} catch( IllegalStateException lvmPerhaps ) {
					log.warn( lvmPerhaps );
					continue;
				}
				BodyFile bf = null;
				try {
					bf = BodyFileBuilder.create( fs );
				} finally {
					fs.close();
				}
				StringWriter sw = new StringWriter();
				BodyFileCodec.format( bf, sw );
				String s = sw.toString();
				String key = "hashfs-" + p.start() + "-" + p.length();
				byte[] value = s.getBytes();
				store.setAttribute( mdd, key, value );
			}
		} finally {
			vs.close();
		}
	}
	
	String storeLocation;
	Store store;
	List<PhysicalDisk> physicalDisks;
	List<VirtualDisk> virtualDisks;
	List<DiskImage> diskImages;
	File tmpDir;
	static boolean verbose, debug;
	Session session;
	VirtualMachineFileSystem vmfs;
	Set<String> vmNames;
	
	static public final String[] PHYSICALDISKNAMES = {
		// Linux/Unix...
		"/dev/sda", "/dev/sdb", "/dev/sdc",
		"/dev/sdd", "/dev/sde", "/dev/sdf",
		// MacOS...
		"/dev/disk0", "/dev/disk1", "/dev/disk2"
	};

	static final String UNMANAGEDDISKREPORTFORMAT = "%2s %42s %16s %16s";

	static final String MANAGEDDISKREPORTFORMAT = "%2s %42s %17s";

	static final String ATTRIBUTEREPORTFORMAT = "%2s %42s";
}


// eof
