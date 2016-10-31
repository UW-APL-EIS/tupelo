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
package edu.uw.apl.tupelo.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Session implements java.io.Serializable,
								Comparable<Session> {

	public Session( UUID source, Calendar c, int i ) {
		this.source = source;
		date = (Calendar)c.clone();
		cropTime( date );
		index = i;
	}

	public boolean equals( Object o ) {
		if( this == o )
			return true;
		if( !(o instanceof Session ) )
			return false;
		Session that = (Session)o;
		return this.source.equals( that.source ) &&
			this.date.equals( that.date ) &&
			this.index == that.index;
	}

	// maintain the general contract for equal Objects, though Map usage unlikely
	public int hashCode() {
		return date.hashCode() + index;
	}
		
	public int compareTo( Session that ) {
		// comparing apples to oranges??
		if( ! this.source.equals( that.source ) )
			return 0;
		int calCmp = this.date.compareTo( that.date );
		if( calCmp == 0 )
			return this.index - that.index;
		return calCmp;
	}
	
	static private void cropTime( Calendar c ) {
		c.set( Calendar.HOUR_OF_DAY, 0 );
		c.set( Calendar.MINUTE, 0 );
		c.set( Calendar.SECOND, 0 );
		c.set( Calendar.MILLISECOND, 0 );
	}

	/**
	   This session's successor, with the current time used as the
	   supplied date parameter
	*/
	public Session successor() {
		return successor( this.date );
	}
	
	/**
	   This session's successor, with supplied cal presumably
	   representing 'now'.  Useful for testing in its own right, and
	   provides the implementation for the no-arg version above
	*/
	public Session successor( Calendar cal ) {
		Calendar date = (Calendar)cal.clone();
		cropTime( date );
		if( this.date.equals( date ) ) {
			return new Session( this.source, this.date, this.index + 1 );
		} else {
			return new Session( this.source, date, 1 );
		}
	}
	
	static public Session parse( String s ) throws ParseException {
		Matcher m = FULLREGEX.matcher( s );
		if( !m.matches() ) {
			throw new ParseException( "Not a FullSessionDescriptor: " + s, 0 );
		}
		UUID uuid = UUID.fromString( m.group(1) );
		SimpleDateFormat sdf = new SimpleDateFormat( DATEFORMAT );
		sdf.setTimeZone( UTC );
		Date d = sdf.parse( m.group(2) );
		Calendar c = Calendar.getInstance( UTC );
		c.setTime( d );
		int i = Integer.parseInt( m.group(3) );
		return new Session( uuid, c, i );
	}

	/*
	  When he string s contains just the 'toString' value, so the uuid
	  must be supplied
	*/
	static public Session parse( UUID source, String s ) throws ParseException {
		Matcher m = SHORTREGEX.matcher( s );
		if( !m.matches() ) {
			throw new ParseException( "Not a ShortSessionDescriptor: " + s, 0 );
		}
		SimpleDateFormat sdf = new SimpleDateFormat( DATEFORMAT );
		sdf.setTimeZone( UTC );
		Date d = sdf.parse( m.group(1) );
		Calendar c = Calendar.getInstance( UTC );
		c.setTime( d );
		int i = Integer.parseInt( m.group(2) );
		return new Session( source, c, i );
	}

	/*
	  public int getIndex() {
		return index;
	}

	public Calendar getDate() {
		return date;
	}
	*/

	public UUID uuid() {
		return source;
	}

	// friendly printable, without the source uuid...
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat( DATEFORMAT );
		sdf.setTimeZone( UTC );
		return sdf.format( date.getTime() ) + "." +
			String.format( "%04d", index );
	}
	
	// the dual of parse, so has to include all fields...
	public String format() {
		return "" + source + "/" + toString();
	}


	static public Session testSession() {
		return new Session( UUID.randomUUID(), Calendar.getInstance(), 16 );
	}
	
	static final String UUIDRE =
		"\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";
	static final String DATERE = "\\d{8}";
	static final String INDEXRE = "\\d{4}";
	
	static public final Pattern FULLREGEX = Pattern.compile
		( "(" + UUIDRE + ")/(" + DATERE + ")\\.(" + INDEXRE + ")" );

	static public final Pattern SHORTREGEX = Pattern.compile
		( "(" + DATERE + ")\\.(" + INDEXRE + ")" );

	static public final String DATEFORMAT = "yyyyMMdd";

	static public TimeZone UTC = TimeZone.getTimeZone( "UTC" );

	static public final UUID NOSOURCE = Constants.NULLUUID;

	static public final Session CANNED = new Session
		( NOSOURCE, Calendar.getInstance(), 1 );

	private final UUID source;
	private final Calendar date;
	private final int index;
}

// eof
