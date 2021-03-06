/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uw.apl.tupelo.http.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.amqp.server.FileHashService;
import edu.uw.apl.tupelo.fuse.ManagedDiskFileSystem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ContextListener implements ServletContextListener {

	public ContextListener() {
		log = LogFactory.getLog( getClass().getPackage().getName() );
	}
	
    public void contextInitialized( ServletContextEvent sce ) {
		log.debug( "ContextInitialized" );

		ServletContext sc = sce.getServletContext();

		try {
			locateVersion( sc );
			locateDataRoot( sc );
			startAMQP( sc );
		} catch( IOException ioe ) {
			log.warn( ioe );
		}
	}

	/**
	   A Maven-based build can be configured (the war plugin) to
	   output various specification/implementation strings into a
	   jar/war. We look for those here.  Alas, it looks like the
	   Package-based inspection approach doesn't work for war files,
	   so we drop down to a more manual way, inspecting the resource
	   META-INF/MANIFEST.MF.

	   @see http://stackoverflow.com/questions/14934299/how-to-get-package-version-at-running-tomcat

	   Testing note: If we are testing the webapp using 'mvn
	   jetty:run', we need to make this 'mvn jetty:run-war', since the
	   former runs the webapp directly from the filesystem, e.g. using
	   src/main/webapp as document root.  And no 'war' packaging is
	   done so the details we are looking for will not exist.
	*/
	private void locateVersion( ServletContext sc ) {
		Package p = getClass().getPackage();
		String version = p.getImplementationVersion();
		if( version == null ) {
			Properties prp = new Properties();
			InputStream is = sc.getResourceAsStream( "/META-INF/MANIFEST.MF" );
			if( is != null ) {
				try {
					prp.load( is );
					is.close();
				} catch( IOException ioe ) {
				}
			}
			version = prp.getProperty( "Implementation-Version" );
		}
		log.info( "Version: " + version );
		sc.setAttribute( VERSIONKEY, version );
	}
	
	private void locateDataRoot( ServletContext sc ) throws IOException {
		String rootS = sc.getInitParameter( DATAROOTKEY );
		if( rootS == null ) {
			String uh = System.getProperty( "user.home" );
			File f = new File( uh );
			f = new File( f, ".tupelo" );
			rootS = f.getPath();
		}
		File dataRoot = new File( rootS ).getCanonicalFile();
		dataRoot.mkdirs();
		sc.setAttribute( DATAROOTKEY, dataRoot );
		log.info( "Store Root: " + dataRoot );
		Store store = new FilesystemStore( dataRoot );
		log.info( "Store UUID: " + store.getUUID() );
		
		sc.setAttribute( STOREKEY, store );
	}

	/**
	 * @return amqp broker url as a string, or null if not found on any of
	 * the possible search locations
	 */
	private String locateBrokerUrl( ServletContext sc ) {
		String result = null;

		/*
		  Search 1: value supplied in context init param list.  Could
		  be hard-wired into web.xml (bad) or truly supplied by
		  container (OK as long as that file NOT under version
		  control).  If non-null, we use.
		*/
		
		if( result == null ) {
			result = sc.getInitParameter( AMQPBROKERKEY );
		}

		/*
		  Search 2: value supplied in a properties object located on
		  the classpath via resource name /tupelo.prp.  Presumably,
		  the build will populate this resource with appropriate
		  values, done via Maven filtered resources (keeps sensitive
		  strings OUT of version- controlled sources).
		*/
		if( result == null ) {
			InputStream is = this.getClass().getResourceAsStream
				( "/tupelo.prp" );
			if( is != null ) {
				try {
					Properties p = new Properties();
					p.load( is );
					is.close();
					result = p.getProperty( AMQPBROKERKEY );
				} catch( IOException ioe ) {
				}
			}
		}
		return result;
	}

	private void startAMQP( ServletContext sc ) throws IOException {
		String brokerURL = locateBrokerUrl( sc );
		if( brokerURL == null ) {
			log.warn( "Missing context param: " + AMQPBROKERKEY );
			log.warn( "Unable to start AMQP Services" );
			return;
		}
		Store s = (Store)sc.getAttribute( STOREKEY );
		final FileHashService fhs = new FileHashService( s, brokerURL );
		sc.setAttribute( AMQPSERVICEKEY, fhs );
		Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						fhs.start();
					} catch( Exception e ) {
						log.warn( e );
					}
				}
			};
		new Thread( r ).start();
	}

	@Override
    public void contextDestroyed( ServletContextEvent sce ) {
		ServletContext sc = sce.getServletContext();
		FileHashService fhs = (FileHashService)sc.getAttribute
			( AMQPSERVICEKEY );
		if( fhs != null ) {
			log.info( "Stopping AMQP service" );
			try {
				fhs.stop();
			} catch( IOException ioe ) {
				log.warn( ioe );
			}
		}
		ManagedDiskFileSystem mdfs = (ManagedDiskFileSystem)sc.getAttribute
			( MDFSOBJKEY );
		if( mdfs != null ) {
			log.info( "Unmounting MDFS" );
			try {
				mdfs.umount();
			} catch( Exception e ) {
				log.warn( e );
			}
		}
		File mdfsMountPoint = (File)sc.getAttribute( MDFSMOUNTKEY );
		if( mdfsMountPoint != null ) {
			log.info( "Deleting MDFS mount point" );
			mdfsMountPoint.delete();
		}
		log.info( "ContextDestroyed" );
	}

	private Log log;
	
	static public final String VERSIONKEY = "version";

	static public final String DATAROOTKEY = "dataroot";
	static public final String STOREKEY = "store";

	static public final String AMQPBROKERKEY = "amqp.url";
	static public final String AMQPSERVICEKEY = "amqpservice";

	// For the ManagedDiskFileSystem object itself...
	static public final String MDFSOBJKEY = "mdfs.obj";

	// For the ManagedDiskFileSystem's mount point, a File object...
	static public final String MDFSMOUNTKEY = "mdfs.mount";
}


// eof
