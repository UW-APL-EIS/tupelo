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
	
	static private final String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS000XXX";

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