package edu.uw.apl.tupelo.logging;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * A log4j Layout, used by an Appender (and recall we have defined an
 * AMQP Appender), providing a log record format as used/requested by
 * Dims/DD.

 * At construction time, we create a uuid. This does essentially same
 * job as a pid, since we only expect one Layout per logging
 * subsystem per VM (or classloader at least)
 */

public class LogMonLayout extends Layout {


	public LogMonLayout() {
		uuid = UUID.randomUUID();
	}
	
    /**
     * format a given LoggingEvent to a string
     * @param loggingEvent
     * @return String representation of LoggingEvent
     */
    @Override
    public String format( LoggingEvent le ) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		writeBasic( le, pw );
		writeThrowable( le, pw );
		return sw.toString();
    }

	private void writeBasic( LoggingEvent le, PrintWriter pw ) {
		SimpleDateFormat sdf = new SimpleDateFormat( ISO8601 );
        pw.print( sdf.format( new Date() ) );
        pw.print( " " );
        pw.print( HOSTNAME );
        pw.print( " " );
		pw.print( uuid );
        pw.print( " " );
		pw.print( le.getLoggerName() );
        pw.print( " " );
		pw.print( le.getLevel() );
        pw.print( " " );
        pw.println( "'" + le.getMessage() + "'" );
	}

	private void writeThrowable( LoggingEvent le, PrintWriter pw ) {
        ThrowableInformation ti = le.getThrowableInformation();
        if( ti == null )
			return;
	}

	
    /**
     * Declares that this layout does not ignore throwable if available
     * @return
     */
    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    /**
     * Just fulfilling the interface/abstract class requirements
     */
    @Override
    public void activateOptions() {
    }


	private final UUID uuid;
	
	static private final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ssZ";

	static private String HOSTNAME = "UNKNOWN";
	static {
		try {
			InetAddress ia = InetAddress.getLocalHost();
			HOSTNAME = ia.getCanonicalHostName();
		} catch( UnknownHostException uhe ) {
		}
	}
		
}

// eof