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
import edu.uw.apl.tupelo.store.Store;


/**
 * A servlet handling miscellaneous requests (uuid,newSession) concerning the
 * attached Tupelo store.
 *
 * We split the 'http store' into many servlets simply to avoid one big servlet.
 *
 * The expected url layout (i.e. path entered into web.xml) for this servlet is
 *
 * /uuid
 * /usablespace
 * /newsession

 */
public class MiscServlet extends HttpServlet {

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
		log = LogFactory.getLog( getClass().getPackage().getName() );

		/*
		  Locate our Store handler from the context.  The
		  bootstrapping ContextListener puts it there
		*/
		ServletContext sc = config.getServletContext();
		store = (Store)sc.getAttribute( ContextListener.STOREKEY );

		// gson object claimed thread-safe, so can be a member...
		GsonBuilder gsonb = new GsonBuilder();
		gsonb.registerTypeAdapter(Session.class, Constants.SESSIONSERIALIZER );
		gson = gsonb.create();

		//		log.info( getClass() + " " + log );
	}
	
	/**
	 * We are mapped to /disks/attr/*, so exactly which operation is
	 * being requested is encoded into the PathInfo
	 */
	public void doGet( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		String sp = req.getServletPath();
		log.debug( "Get.ServletPath: " + sp );
		String pi = req.getPathInfo();
		log.debug( "Get.PathInfo: " + pi );

		if( sp.equals( "/version" ) ) {
			version( req, res );
			return;
		}

		if( sp.equals( "/uuid" ) ) {
			uuid( req, res );
			return;
		}
		if( sp.equals( "/usablespace" ) ) {
			usableSpace( req, res );
			return;
		}

		/*
		  LOOK: Should we allow GETs for content that by definition
		  changes on each request??
		*/
		if( sp.equals( "/newsession" ) ) {
			newSession( req, res );
			return;
		}

	}
	
	/**
	 * We are mapped to /disks/attr/*, so exactly which operation is
	 * being requested is encoded into the PathInfo
	 */
	public void doPost( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {
		
		String sp = req.getServletPath();
		log.debug( "Post.ServletPath: " + sp );
		String pi = req.getPathInfo();
		log.debug( "Post.PathInfo: " + pi );


		if( sp.equals( "/newsession" ) ) {
			newSession( req, res );
			return;
		}
	}
	
		
	private void version( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		ServletContext sc = getServletConfig().getServletContext();
		String result = (String)sc.getAttribute( ContextListener.VERSIONKEY );
		
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

	private Store store;
	private Gson gson;
	private Log log;

}

// eof

