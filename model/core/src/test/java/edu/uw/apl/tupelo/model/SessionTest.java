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

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

/**
 * @author Stuart Maclean
 *
 * Tests on Session objects.
 */

public class SessionTest extends junit.framework.TestCase {

	public void testSuccessor() {
		System.out.println( "testSuccessor" );
		Calendar now = Calendar.getInstance( Session.UTC );
		Session s1 = new Session( Session.NOSOURCE, now, 1 );
		System.out.println( s1.format() );

		Session s2 = s1.successor();
		System.out.println( s2.format() );

		Calendar tomorrow = Calendar.getInstance( Session.UTC );
		tomorrow.add( Calendar.DATE, 1 );
		Session s3 = s1.successor( tomorrow );
		System.out.println( s3.format() );

		Session s4 = s2.successor();
		System.out.println( s4.format() );
		
		Session s5 = s1.successor();
		System.out.println( s5.format() );
		
		Session s6 = s3.successor();
		System.out.println( s6.format() );
		
		Session s7 = s3.successor( tomorrow );
		System.out.println( s7.format() );
	}

	/**
	 * Testing Session.equals
	 */
	public void testEquals() throws Exception {
		System.out.println( "testEquals" );

		Calendar now = Calendar.getInstance( Session.UTC );
		Session s1 = new Session( Session.NOSOURCE, now, 1 );
		Session s2 = new Session( Session.NOSOURCE, now, 1 );
		assertTrue( s1.equals(s2) );

		Session s3 = new Session( Session.NOSOURCE, now, 2 );
		assertFalse( s1.equals(s3) );
	}

	/**
	 * Testing Session.compareTo
	 */
	public void testCompare() throws Exception {
		System.out.println( "testCompare" );

		Calendar now = Calendar.getInstance( Session.UTC );
		Session s1 = new Session( Session.NOSOURCE, now, 1 );
		Session s2 = new Session( Session.NOSOURCE, now, 1 );
		assertTrue( 0 == s1.compareTo( s2 ) );

		Session s3 = new Session( Session.NOSOURCE, now, 2 );
		assertTrue( -1 == s1.compareTo( s3 ) );
		assertTrue( +1 == s3.compareTo( s1 ) );

		Calendar tomorrow = (Calendar)now.clone();
		tomorrow.add( Calendar.HOUR_OF_DAY, 24 );
		Session s4 = new Session( Session.NOSOURCE, tomorrow, 1 );
		assertTrue( -1 == s1.compareTo( s4 ) );
		assertTrue( -1 == s2.compareTo( s4 ) );
		assertTrue( -1 == s3.compareTo( s4 ) );
	}
	
	public void testSerial() throws Exception {
		System.out.println( "testSerial" );
		Calendar now = Calendar.getInstance( Session.UTC );
		Session s1 = new Session( Session.NOSOURCE, now, 1 );
		String f1 = s1.format();
		Session s2 = Session.parse( f1 );
		System.out.println( s1.format() );
		System.out.println( s2.format() );
		assertTrue( "", s2.format().equals( s1.format() ) );
	}

	public void testOrder() {
		System.out.println( "testOrder" );
		List<Session> ss = new ArrayList<>();
		Calendar today = Calendar.getInstance( Session.UTC );
		UUID u = UUID.randomUUID();
		Session s1 = new Session( u, today, 1 );
		ss.add( s1 );
		Session s2 = new Session( u, today, 5 );
		ss.add( s2 );
		Session s3 = new Session( u, today, 3 );
		ss.add( s3 );

		Calendar tomorrow = (Calendar)today.clone();
		tomorrow.add( Calendar.DATE, 1 );
		Session s4 = new Session( u, tomorrow, 3 );
		ss.add( s4 );

		Calendar yesterday = (Calendar)today.clone();
		yesterday.add( Calendar.DATE, -1 );
		Session s5 = new Session( u, yesterday, 3 );
		ss.add( s5 );

		Session s6 = new Session( u, yesterday, 1 );
		ss.add( s6 );

		Session s7 = new Session( u, tomorrow, 1 );
		ss.add( s7 );

		Session s8 = new Session( u, today, 1 );
		ss.add( s8 );

		Collections.sort( ss );
		for( Session s : ss ) 
			System.out.println( s.format() );
	}
}

// eof
