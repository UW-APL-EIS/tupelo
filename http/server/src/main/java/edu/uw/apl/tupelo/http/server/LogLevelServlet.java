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

