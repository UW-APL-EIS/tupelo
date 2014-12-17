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
   A webapp front-end for a Store object.  Note how there is NO
   'business logic' here, its all just marshalling of parameters and
   Store operations.  A webapp should never do any more than this.
*/
public class HttpStoreServlet extends HttpServlet {

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
		log = LogFactory.getLog( getClass() );

		/*
		  locate our Store handler from the context.  The bootstrapping
		  ContextListener puts it there
		*/
		ServletContext sc = config.getServletContext();
		store = (Store)sc.getAttribute( ContextListener.STOREKEY );

		// gson object claimed thread-safe, so can be a member...
		GsonBuilder gsonb = new GsonBuilder();
		gsonb.registerTypeAdapter(Session.class, new SessionSerializer());
		gson = gsonb.create();
	}
	
	public void doGet( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		// Step 1: match against the servlet path
		String sp = req.getServletPath();
		if( sp.equals( "/uuid" ) ) {
			uuid( req, res );
			return;
		}
		if( sp.equals( "/usablespace" ) ) {
			usableSpace( req, res );
			return;
		}

		if( sp.equals( "/newsession" ) ) {
			newSession( req, res );
			return;
		}

		if( sp.equals( "/enumerate" ) ) {
			enumerate( req, res );
			return;
		}

		if( sp.startsWith( "/digest/" ) ) {
			String pi = req.getPathInfo();
			log.debug( "Get.PathInfo: " + pi );
			digest( req, res );
			return;
		}

		if( sp.startsWith( "/getAttribute/" ) ) {
			String pi = req.getPathInfo();
			log.debug( "Get.PathInfo: " + pi );
			getAttribute( req, res );
			return;
		}

		// Step 2: match against the path info
		String pi = req.getPathInfo();
		System.out.println( "PathInfo: " + pi );
		log.debug( "Get.PathInfo: " + pi );
		if( false ) {
		} else if( pi.equals( "/listvolumes" ) ) {
			//listVolumes( req, res );
		} else if( pi.equals( "/list" ) ) {
			//listVolumes( req, res );
		} else if( pi.startsWith( "/getdescriptors/" ) ) {
			//getDescriptors( req, res );
		} else if( pi.startsWith( "/getdatadescriptors/" ) ) {
			//getDataDescriptors( req, res );
		} else if( pi.startsWith( "/getvolumesystem/" ) ) {
			//getVolumeSystem( req, res );
		} else if( pi.startsWith( "/getvolume/" ) ) {
			//getVolume( req, res );
		} else if( pi.startsWith( "/getdevice/" ) ) {
			//getDevice( req, res );
		} else if( pi.startsWith( "/listattributes/" ) ) {
			//listAttributes( req, res );
		} else if( pi.startsWith( "/getattribute/" ) ) {
			//getAttribute( req, res );
		} else {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Unknown command '" + pi + "'" );
			return;
		}
	}
	
	public void doPost( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {
		
		// Step 1: match against the servlet path
		String sp = req.getServletPath();
		if( sp.equals( "/newsession" ) ) {
			newSession( req, res );
			return;
		}
		
		if( sp.startsWith( "/data/put/" ) ) {
			String pi = req.getPathInfo();
			log.debug( "Post.PathInfo: " + pi );
			put( req, res );
			return;
		}

		// Step 2: match against the path info
		String pi = req.getPathInfo();
		System.out.println( "PathInfo: " + pi );
		log.debug( "Post.PathInfo: " + pi );
		if( false ) {
		} else if( pi.equals( "/newsession" ) ) {
			// newsession as a POST
			newSession( req, res );
		} else if( pi.startsWith( "/put/" ) ) {
			put( req, res );
		} else if( pi.startsWith( "/putdevice/" ) ) {
			//putDevice( req, res );
		} else if( pi.startsWith( "/putattributes/" ) ) {
			//putAttributes( req, res );
		} else {
			res.sendError( HttpServletResponse.SC_NOT_FOUND,
						   "Unknown command '" + pi + "'" );
			return;
		}
	}

	private void uuid( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		UUID result = store.getUUID();
		
		if( false ) {
		} else if( Utils.acceptsJavaObjects( req ) ) {
			res.setContentType( "application/x-java-serialized-object" );
			OutputStream os = res.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream( os );
			oos.writeObject( result );
		} else if( Utils.acceptsJson( req ) ) {
			res.setContentType( "application/json" );
			String json = gson.toJson( result );
			PrintWriter pw = res.getWriter();
			pw.print( json );
		} else {
			res.setContentType( "text/plain" );
			PrintWriter pw = res.getWriter();
			pw.println( "" + result );
		}
	}

	private void usableSpace( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		long result = store.getUsableSpace();
		
		if( false ) {
		} else if( Utils.acceptsJavaObjects( req ) ) {
			res.setContentType( "application/x-java-serialized-object" );
			OutputStream os = res.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream( os );
			oos.writeObject( result );
		} else if( Utils.acceptsJson( req ) ) {
			res.setContentType( "application/json" );
			String json = gson.toJson( result );
			PrintWriter pw = res.getWriter();
			pw.print( json );
		} else {
			res.setContentType( "text/plain" );
			PrintWriter pw = res.getWriter();
			pw.println( "" + result );
		}
	}

	private void newSession( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {
		
		// NOTHING to do with an HttpSession!
		Session session = store.newSession();

		if( false ) {
		} else if( Utils.acceptsJavaObjects( req ) ) {
			res.setContentType( "application/x-java-serialized-object" );
			OutputStream os = res.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream( os );
			oos.writeObject( session );
		} else if( Utils.acceptsJson( req ) ) {
			res.setContentType( "application/json" );
			String json = gson.toJson( session );
			PrintWriter pw = res.getWriter();
			pw.print( json );
		} else {
			res.setContentType( "text/plain" );
			PrintWriter pw = res.getWriter();
			pw.println( session.format() );
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
			pw.println( "TODO: Store.enumerate" );
		}
		
	}
	
	/**
	   Assumed that req already interrogated to derive put (managed
	   disk) as the required action.

	   diskId and session component values are encoded in the pathInfo:

	   put/diskID/session
	   
	   @see HttpStoreProxy for a corresponding client
	*/
	private void put( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		// to get the details, need the pathInfo again...
		String pi = req.getPathInfo();
		String details = pi.substring( "/put/".length() );

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

	private void digest( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		// to get the details, need the pathInfo again...
		String pi = req.getPathInfo();
		String details = pi;

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

		Object digest = store.digest( mdd );
		
		
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

	private void getAttribute( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		// to get the details, need the pathInfo again...
		String pi = req.getPathInfo();
		String details = pi;

		log.debug( "getAttribute.details: '" + details  + "'" );

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

		String key = "key";
			
		byte[] value = store.getAttribute( mdd, key );
		
		
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
			oos.writeObject( value );
		} else if( Utils.acceptsJson( req ) ) {
			res.setContentType( "application/json" );
			String json = gson.toJson( value );
			PrintWriter pw = res.getWriter();
			pw.print( json );
		} else {
			res.setContentType( "text/plain" );
			PrintWriter pw = res.getWriter();
			pw.println( "TODO: Store.getAttribute text/plain" );
		}

	}

	/*
	  LOOK: we assume the client did NOT send the full session info,
	  including the uuid, because that session must have generated by
	  us anyway ??
	*/
	ManagedDiskDescriptor fromPathInfo( String pathInfo )
		throws ParseException, IOException {

		log.debug( MDDPIREGEX.pattern( ) );
				   
		Matcher m = MDDPIREGEX.matcher( pathInfo );
		if( !m.matches() ) {
			throw new ParseException( "Malformed managed disk path: " +
									  pathInfo,0 );
		}
		String diskID = m.group(1);
		Session s = Session.parse( store.getUUID(), m.group(2) );
		return new ManagedDiskDescriptor( diskID, s );
	}

	// A ManagedDiskDescriptor (diskid,session) encoded in a url path info
	static public final Pattern MDDPIREGEX = Pattern.compile
		( "(" + ManagedDiskDescriptor.DISKIDREGEX.pattern() + ")/" +
		  "(" + Session.SHORTREGEX.pattern() + ")" );

	private Store store;
	private Gson gson;
	private Log log;

	// for Json output of Session objects, we just use their .format method...
	static class SessionSerializer implements JsonSerializer<Session> {
		public JsonElement serialize( Session src,
									  java.lang.reflect.Type typeOfSrc,
									  JsonSerializationContext context) {
			return new JsonPrimitive(src.format());
		}
	}
}

// eof

