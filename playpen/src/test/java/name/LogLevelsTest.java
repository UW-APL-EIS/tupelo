package name;

import org.apache.log4j.*;
import org.apache.commons.logging.*;

/**
 * Result: If we want many classes in a package to share a logger, and
 * thus ALL see the results of a single setLevel() call, all loggers
 * MUST be declared with the SAME string. so THIS works:
 *
 * log = Logger.getLogger( getClass().getPackage().getName() );
 *
 * but this does NOT work (since individual loggers are created for each class
 * and setting one's level does NOT affect any others)
 *
 * log = Logger.getLogger( getClass() );
 */
public class LogLevelsTest extends junit.framework.TestCase {

	static class A {
		A() {

			/*
			  This is what NOT to do if you want all classes to use SAME
			  logger and so all 'see' the effects of a setLevel() call
			*/
			//			log = Logger.getLogger( getClass() );


			/*
			  This IS what to do, since all log creations then use the SAME
			  string
			*/
			log = Logger.getLogger( getClass().getPackage().getName() );

			log.debug( log.getName() );
		}
		protected Logger log;
	}

	static class B extends A {
	}

	static class C extends A {
	}

	static class D {
		D() {
			log = LogFactory.getLog( getClass().getPackage().getName() );
			log.debug( "D" );
		}
		Log log;
	}
	
	public void testNull() {
	}

	public void testSharedLevels() {
		B b = new B();
		C c = new C();
		D d = new D();
		
		System.out.println( b.log.getLevel() );
		System.out.println( c.log.getLevel() );

		System.out.println( b.log.getEffectiveLevel() );
		System.out.println( c.log.getEffectiveLevel() );

		b.log.setLevel( Level.WARN );

		System.out.println( b.log.getLevel() );
		System.out.println( c.log.getLevel() );

		System.out.println( b.log.getEffectiveLevel() );
		System.out.println( c.log.getEffectiveLevel() );

		d.log.debug( "D.debug" );
		d.log.info( "D.info" );
		d.log.warn( "D.warn" );
	}
}

// eof
