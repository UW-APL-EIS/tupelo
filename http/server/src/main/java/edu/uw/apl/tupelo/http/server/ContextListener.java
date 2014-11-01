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
	   A Maven build buries a properties file in the war.  This prop
	   file contains the Maven version of the module, which we can use
	   to identify ourselves.

	   Testing note: if we are testing the webapp using 'mvn
	   jetty:run', we need to make this 'mvn jetty:run-war', since the
	   former runs the webapp directly from the filesystem, e.g. using
	   src/main/webapp as document root.  And no 'war' packaging is
	   done so the prop we are looking for will not exist.
	*/
	private void locateVersion( ServletContext sc ) {
		String version = "unknown";
		String mavenGroupID = "edu.uw.apl.mwa";
		String mavenArtifactID = "tupelo-webapp-server";
		String mavenPomProperties =	"/META-INF/maven/" + mavenGroupID +
			"/" + mavenArtifactID + "/pom.properties";
		InputStream is = sc.getResourceAsStream( mavenPomProperties );
		if( is != null ) {
			Properties p = new Properties();
			try {
				p.load( is );
				is.close();
				String value = p.getProperty( "version" );
				if( value != null )
					version = value;
			} catch( IOException ioe ) {
				System.err.println( ioe );
			}
		}
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
		/*
		  Properties p = new Properties();
		try {
			FileInputStream fis = new FileInputStream( configS );
			p.load( fis );
			fis.close();
		} catch( IOException ioe ) {
			log.warn( ioe );
		}
		String rootS = p.getProperty( "root" );
		if( rootS == null ) {
			String uh = System.getProperty( "user.home" );
			rootS = uh + "/.manuka2/server";
		}
		*/
		//		Logger log = Logger.getLogger( getClass().getPackage().getName() );
		/*
		  Logger log = Logger.getLogger( "edu.uw.apl.mwa.tupelo" );
		log.setLevel( Level.DEBUG );
		log.setAdditivity( false );
		*/
		
		/*
		  try {
			File logDir = new File( dataRoot, "logs" );
			File logFile = new File( logDir, "webapp.log" );
			FileAppender fa = new FileAppender( new SimpleLayout(),
												logFile.getPath() );
			log.addAppender( fa );
		} catch( IOException ioe ) {
			System.err.println( ioe );
		}
		*/
		
		//		File root = new File( dataRoot, "server" );
		//		log.info( "DataStore root: " + root );

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
