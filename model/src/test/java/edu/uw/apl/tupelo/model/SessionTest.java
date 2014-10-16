package edu.uw.apl.tupelo.model;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

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
		List<Session> ss = new ArrayList<Session>();
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
