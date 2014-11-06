package edu.uw.apl.tupelo.http.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

public class PathsTest extends junit.framework.TestCase {

	public void testDiskID() {
		String did = "32m";

		Pattern p = ManagedDiskDescriptor.DISKIDREGEX;
		Matcher m = p.matcher( did );
		assert( m.matches() );
		
	}

	public void testSession() {
		Session s = Session.CANNED;
		String f = "" + s;

		Pattern p = Session.SHORTREGEX;
		Matcher m = p.matcher( f );
		assert( m.matches() );
	}

	public void testPathInfo1() {
		String pi = "32m/12345678.1234";
		Pattern p = HttpStoreServlet.MDDPIREGEX;
		Matcher m = p.matcher( pi );
		assert( m.matches() );
	}

	public void testPathInfo2() {
		String pi = "32m/20141023.0019";
		Pattern p = HttpStoreServlet.MDDPIREGEX;
		Matcher m = p.matcher( pi );
		assert( m.matches() );
	}
	
}

// eof
