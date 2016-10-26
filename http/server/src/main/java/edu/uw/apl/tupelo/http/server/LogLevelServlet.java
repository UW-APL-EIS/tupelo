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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * I have always wanted to do this.  Affect the log level of the
 * loggers used BY the web app by way of a url request sent TO the
 * webapp.  'Edit the log4j.properties file and reload the war' - just
 * say no!
 *
 * Likely used in the scenario 'The web site is not working.  Do a
 * wget/curl to change the log level and monitor the amqp message bus
 * (since we have a rabbitmq appender configured)'. Note that we
 * respond to POST, not GET.
 *
 * One gotcha: Apache commons logging, though a nice facade for log4j,
 * provides no way to set a log level.  So here, and here only, we use
 * log4j classes directly, pah! Elsewhere in this web app, we use
 * commons logging (why?!)
 *
 * The expected url layout (i.e. path entered into web.xml) for this
 * servlet is
 *
 * /logging/debug
 * /logging/info, etc
 */
public class LogLevelServlet extends HttpServlet {

	@Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
		/*
		  All servlets in this web app use the SAME logger, named
		  in the log4j.properties by our package name
		*/
		log = Logger.getLogger( getClass().getPackage().getName() );
		//		log.info( getClass() + " " + log );
	}
	
	@Override
	public void doPost( HttpServletRequest req, HttpServletResponse res )
		throws IOException, ServletException {

		String sp = req.getServletPath();
		log.debug( "Post.ServletPath: " + sp );
		String pi = req.getPathInfo();
		log.debug( "Post.PathInfo: " + pi );
		String details = pi.substring(1);
		Level current = log.getEffectiveLevel();
		Level next = Level.toLevel( details, current );
		/*
		if( false ) {
		} else if( "debug".equalsIgnoreCase( details ) ) {
			l = Level.DEBUG;
		} else if( "info".equalsIgnoreCase( details ) ) {
			l = Level.INFO;
		} else if( "message".equalsIgnoreCase( details ) ) {
			l = Level.MESSAGE;
		}
		*/
		log.setLevel( next );
		log.info( current + " -> " + next );

	}
	
	private Logger log;
}

// eof

