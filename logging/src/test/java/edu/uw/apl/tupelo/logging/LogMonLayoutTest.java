package edu.uw.apl.tupelo.logging;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

/**
 * See src/test/resources/log4j.properties for the set up of this
 * Layout test.  Hard to assert as a unit test it succeeds/fails.
 * Rather, for each logged message, we expect to see
 *
 * The time, in ISO8601 format
 *
 * The host name of the machine on which the JVM is running
 *
 * The logger name, which always equates to the class producing the message
 *
 * The log level, e.g. DEBUG, INFO
 *
 * The message being logged
 */
public class LogMonLayoutTest extends junit.framework.TestCase {

	protected void setUp() {
		log = Logger.getLogger( getClass() );
	}
	
	protected void tearDown() {
		LogManager.shutdown();
	}
	
	public void test1() {
		log.debug( "Test Message" );
	}

	Logger log;
}

// eof
