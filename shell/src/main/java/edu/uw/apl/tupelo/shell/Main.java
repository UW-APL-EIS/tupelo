package edu.uw.apl.tupelo.shell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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
					} catch( RuntimeException re ) {
						System.err.println( re.getMessage() );
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
					} catch( RuntimeException re ) {
						System.err.println( re.getMessage() );
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
					} catch( RuntimeException iae ) {
						System.err.println( iae );
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
		report( "Store: " + storeLocation );
		identifyUnmanagedDisks();
		super.start();
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
			throw new IllegalArgumentException( "Index out-of-range: " +
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
									   "N", "ID", "Size", "Path" );
		System.out.println( header );
		reportPhysicalDisks( 1 );
		reportVirtualDisks( 1 + physicalDisks.size() );
	}

	void reportPhysicalDisks( int n ) {
		for( PhysicalDisk pd : physicalDisks ) {
			String fmt = String.format( UNMANAGEDDISKREPORTFORMAT,
										n, pd.getID(), pd.size(), pd.getSource() );
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
		System.out.println( "Matching data: " + matching );

		ManagedDiskDigest digest = null;
		UUID uuid = null;
		if( !matching.isEmpty() ) {
			ManagedDiskDescriptor recent = matching.get( matching.size()-1 );
			log.info( "Retrieving uuid for: "+ recent );
			uuid = store.uuid( recent );
			System.out.println( "UUID: " + uuid );
			log.info( "Retrieving digest for: "+ recent );
			digest = store.digest( recent );
			if( digest != null )
				System.out.println( "Digest: " + digest.size() );
			
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
			store.put( md, cb, 5 );
		}
		store.setAttribute( mdd, "path", ud.getSource().getPath().getBytes() );
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

	private void hashFileSystemsNonVirtual( UnmanagedDisk ud ) throws IOException {
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

	static final String MANAGEDDISKREPORTFORMAT = "%42s %17s";
}


// eof
