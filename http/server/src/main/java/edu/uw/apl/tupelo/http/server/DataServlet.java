package edu.uw.apl.tupelo.http.server;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.UUID;


import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonPrimitive;

import edu.uw.apl.tupelo.model.ManagedDisk;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A servlet handling just requests (put,get) about managed disk
 * data itself.
 *
 * We split the 'http store' into many servlets simply to avoid one big servlet.
 *
 * The expected url layout (i.e. path entered into web.xml) for this servlet is
 *
 * /disks/data/enumerate
 * /disks/data/put/DID/SID
 * /disks/data/digest/DID/SID
 *
 *
 * /disks/data/get/DID/SID (todo, currently no support for retrieving managed data)
 *
 */
public class DataServlet extends HttpServlet {

	@Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
		log = LogFactory.getLog( getClass() );

		/*
		  locate our Store handler from the context.  The bootstrapping
		  ContextListener puts it there
		*/
		ServletContext sc = config.getServletContext();
		store = (Store)sc.getAttribute( ContextListener.STOREKEY );
	}
	
	@Override
	public void doGet( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		String sp = req.getServletPath();
		log.debug( "Get.ServletPath: " + sp );
		String pi = req.getPathInfo();
		log.debug( "Get.PathInfo: " + pi );

		if( false ) {
		} else if( pi.equals( "/enumerate" ) ) {
			enumerate( req, res );
		} else if( pi.startsWith( "/digest/" ) ) {
			String details = pi.substring( "/digest/".length() );
			digest( req, res, details );
		} else {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Unknown command '" + pi + "'" );
			return;
		}
	}
	
	/**
	 * We are mapped to /disks/data/*, so exactly which operation is
	 * being requested is encoded into the PathInfo
	 */
	@Override
	public void doPost( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {
		
		String sp = req.getServletPath();
		log.debug( "Post.ServletPath: " + sp );
		String pi = req.getPathInfo();
		log.debug( "Post.PathInfo: " + pi );

		if( false ) {
		} else if( pi.startsWith( "/put/" ) ) {
			String details = pi.substring( "/put/".length() );
			putData( req, res, details );
		} else {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Unknown command '" + pi + "'" );
			return;
		}
	}

	private void enumerate( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		
		if( false ) {
		} else if( Utils.acceptsJavaObjects( req ) ) {
			res.setContentType( "application/x-java-serialized-object" );
			OutputStream os = res.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream( os );

			/*
			  Having serialization issues with the object returned
			  from the store, what seems to be a HashMap$KeySet.  So
			  expand to a regular List on output.
			*/
			List<ManagedDiskDescriptor> asList =
				new ArrayList<ManagedDiskDescriptor>( mdds );
			oos.writeObject( asList );
		} else if( Utils.acceptsJson( req ) ) {
			res.setContentType( "application/json" );
			String json = gson.toJson( mdds );
			PrintWriter pw = res.getWriter();
			pw.print( json );
		} else {
			res.setContentType( "text/plain" );
			PrintWriter pw = res.getWriter();
			for( ManagedDiskDescriptor mdd : mdds ) {
				pw.println( mdd.toString() );
			}
			pw.close();
		}
		
	}

	private void putData( HttpServletRequest req, HttpServletResponse res,
						  String details )
		throws IOException, ServletException {

		log.debug( "Put.details: '" + details  + "'" );

		ManagedDiskDescriptor mdd = null;
		try {
			mdd = fromPathInfo( details );
		} catch( ParseException pe ) {
			log.debug( "put send error" );
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Malformed managed disk descriptor: " + details );
			return;
		}

		// LOOK: check the content type...
		String hdr = req.getHeader( "Content-Encoding" );

		log.debug( "MDD: " + mdd );
		InputStream is = req.getInputStream();
		ManagedDisk md = new HttpManagedDisk( mdd, is );
		store.put( md );
		is.close();
	}

	private void digest( HttpServletRequest req, HttpServletResponse res,
						 String details )
		throws IOException, ServletException {
		
		log.debug( "digest.details: '" + details  + "'" );

		ManagedDiskDescriptor mdd = null;
		try {
			mdd = fromPathInfo( details );
		} catch( ParseException pe ) {
			log.debug( "put send error" );
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Malformed managed disk descriptor: " + details );
			return;
		}

		// LOOK: check the content type...
		String hdr = req.getHeader( "Content-Encoding" );

		List<byte[]> digest = store.digest( mdd );
		
		
		if( false ) {
		} else if( Utils.acceptsJavaObjects( req ) ) {
			res.setContentType( "application/x-java-serialized-object" );
			OutputStream os = res.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream( os );
			oos.writeObject( digest );
		} else if( Utils.acceptsJson( req ) ) {
			res.setContentType( "application/json" );
			String json = gson.toJson( digest );
			PrintWriter pw = res.getWriter();
			pw.print( json );
		} else {
			res.setContentType( "text/plain" );
			PrintWriter pw = res.getWriter();
			pw.println( "TODO: Store.digest text/plain" );
		}

	}

	private ManagedDiskDescriptor fromPathInfo( String pathInfo )
		throws ParseException, IOException {

		Matcher m = Constants.MDDPIREGEX.matcher( pathInfo );
		if( !m.matches() ) {
			throw new ParseException( "Malformed managed disk path: " +
									  pathInfo,0 );
		}
		String diskID = m.group(1);
		Session s = Session.parse( store.getUUID(), m.group(2) );
		return new ManagedDiskDescriptor( diskID, s );
	}

	private Store store;
	private Gson gson;
	private Log log;
}

// eof

