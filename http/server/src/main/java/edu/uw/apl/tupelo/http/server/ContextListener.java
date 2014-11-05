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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ContextListener implements ServletContextListener {

	public ContextListener() {
		log = LogFactory.getLog( getClass() );
	}
	
    public void contextInitialized( ServletContextEvent sce ) {
		log.debug( "ContextInitialized" );

		ServletContext sc = sce.getServletContext();

		try {
			locateVersion( sc );
			locateDataRoot( sc );
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
		log.info( "Store Root: " + dataRoot );
		Store store = new FilesystemStore( dataRoot );
		
		sc.setAttribute( STOREKEY, store );
	}

	@Override
    public void contextDestroyed( ServletContextEvent sce ) {
		log.info( "ContextDestroyed" );
	}

	private Log log;
	
	static public final String VERSIONKEY = "version";
	static public final String DATAROOTKEY = "dataroot";
	static public final String STOREKEY = "store";
}


// eof
