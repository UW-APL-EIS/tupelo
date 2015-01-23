package edu.uw.apl.tupelo.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonPrimitive;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;


/**
 * A servlet handling just requests which run processing tools against
 * managed disks held in the attached store.  Examples include
 *
 * digest computation
 * file system hashing to bodyfiles
 * unallocated area hashing
 *
 * The expected url layout (i.e. path entered into web.xml) for this servlet is
 *
 * /tools/
 * /tools/NAME/DID/SID/
 *
 * for the various tool NAMES: digest, hashfs, hashvs.
 *
 * The first url produces a 'matrix' of all managed disks and tool
 * names, for trivial point-and-click tool invocation.

 * All other urls invoke methods and should be done via POST.  They
 * are started in a newly spawned thread, which outlives the http
 * request.  The requests returns nothing.
 */
public class ToolsServlet extends HttpServlet {

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
		log = LogFactory.getLog( getClass().getPackage().getName() );

		/*
		  Locate our Store handler from the context.  The
		  bootstrapping ContextListener puts it there
		*/
		ServletContext sc = config.getServletContext();

		/*
		  Unlike other servlets which use just the Store api, we HAVE to
		  view it as a FilesystemStore, to be able to invoke e.g. computeDigest()
		*/
		store = (FilesystemStore)sc.getAttribute( ContextListener.STOREKEY );

		// gson object claimed thread-safe, so can be a member...
		GsonBuilder gsonb = new GsonBuilder();
		gsonb.registerTypeAdapter(Session.class, Constants.SESSIONSERIALIZER );
		gson = gsonb.create();

		//		log.info( getClass() + " " + log );

	}
	
	public void doGet( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {
		
		String sp = req.getServletPath();
		log.debug( "Post.ServletPath: " + sp );
		String pi = req.getPathInfo();
		log.debug( "Post.PathInfo: " + pi );

		if( false ) {
		} else if( pi == null ) {
			list( req, res );
		} else {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Unknown command '" + pi + "'" );
		}
	}

	/**
	 * We are mapped to /disks/tools/*, so exactly which operation is
	 * being requested is encoded into the PathInfo
	 */
	public void doPost( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {
		
		String sp = req.getServletPath();
		log.debug( "Post.ServletPath: " + sp );
		String pi = req.getPathInfo();
		log.debug( "Post.PathInfo: " + pi );

		if( false ) {
		} else if( pi.startsWith( "/digest/" ) ) {
			String details = pi.substring( "/digest/".length() );
			computeDigest( req, res, details );
		} else if( pi.startsWith( "/hashvs/" ) ) {
			String details = pi.substring( "/hashvs/".length() );
			hashVolumeSystem( req, res, details );
		} else {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Unknown command '" + pi + "'" );
			return;
		}
	}

	private void list( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		//		System.out.println( "Tools.list" );

		// We are the model, we formulate the data...
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		ArrayList<ManagedDiskDescriptor> sorted = new ArrayList<ManagedDiskDescriptor>
			( mdds );
		Collections.sort( sorted, ManagedDiskDescriptor.DEFAULTCOMPARATOR );
		req.setAttribute( "mdds", mdds );

		// and delegate the JSP view for rendering, classic MVC
		RequestDispatcher rd = req.getRequestDispatcher( "./tools.jsp" );
		rd.forward( req, res );
	}
	
	private void computeDigest( HttpServletRequest req, HttpServletResponse res,
							   final String details )
		throws IOException, ServletException {

		log.debug( "computeDigest.details: '" + details  + "'" );

		Matcher m = Constants.MDDPIREGEX.matcher( details );
		if( !m.matches() ) {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Malformed managed disk descriptor: " + details );
			return;
		}
		String diskID = m.group(1);
		Session s = null;
		try {
			s = Session.parse( store.getUUID(), m.group(2) );
		} catch( ParseException notAfterRegexMatch ) {
		}
	   final ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( diskID, s );

		Runnable r = new Runnable() {
				public void run() {
					try {
						store.computeDigest( mdd );
					} catch( Exception e ) {
						log.warn( details + " -> " + e );
					}
				}
			};
		new Thread( r ).start();
	}

	private void hashVolumeSystem( HttpServletRequest req, HttpServletResponse res,
								   final String details )
		throws IOException, ServletException {
		
		log.debug( "hashVolumeSystem.details: '" + details  + "'" );

		Matcher m = Constants.MDDPIREGEX.matcher( details );
		if( !m.matches() ) {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Malformed managed disk descriptor: " + details );
			return;
		}
		String diskID = m.group(1);
		Session s = null;
		try {
			s = Session.parse( store.getUUID(), m.group(2) );
		} catch( ParseException notAfterRegexMatch ) {
		}
	   final ManagedDiskDescriptor mdd = new ManagedDiskDescriptor( diskID, s );

		Runnable r = new Runnable() {
				public void run() {
					try {
						//						store.computeDigest( mdd );
					} catch( Exception e ) {
						log.warn( details + " -> " + e );
					}
				}
			};
		new Thread( r ).start();
	}

	private FilesystemStore store;
	private Gson gson;
	private Log log;

}

// eof

