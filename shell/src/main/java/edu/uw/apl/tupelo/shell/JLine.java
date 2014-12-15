package edu.uw.apl.tupelo.shell;

/*
  For the purposes of creating a runnable jar with Elvis
  invoked by jline.ConsoleRunner, providing very nice command line
  editing features.
*/
public class JLine {

	static public void main( String[] args ) throws Exception {
		String[] args2 = new String[args.length+1];
		args2[0] = JLine.class.getPackage().getName() + ".Main";
		for( int i = 0; i < args.length; i++ )
			args2[i+1] = args[i];
		jline.ConsoleRunner.main( args2 );
	}
}

// eof

