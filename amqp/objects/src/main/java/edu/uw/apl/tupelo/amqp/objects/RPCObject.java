package edu.uw.apl.tupelo.amqp.objects;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import com.google.gson.reflect.TypeToken;

/**
 * DD wants all JSON structures put on the AMQP bus to have this basic content.
 *
 * @see http://foswiki.prisem.washington.edu/Development/AMQP_RPC#AMQP_for_Remote_Procedure_Calling
 *
 * So we wrap our actual data, of type T, in instances of this class,
 * which supplies the extra data requested.
 */
public class RPCObject<T>  {
	
	static public <T> RPCObject<T> asRPCObject( T t ) {
		return asRPCObject( t, t.getClass().getName() );
	}

	static public <T> RPCObject<T> asRPCObject( T t, String name ) {
		return new RPCObject<T>( t, name );
	}

	/*
	  Not sure how to achieve this yet, do it at the deserialize site...
	  
	static public <T> Type getType( TypeVariable<T> t ) {
		return new TypeToken<RPCObject<t>>(){}.getType();
	}

	static public <T> Type getType( T t ) {
		return new TypeToken<RPCObject<T>>(){}.getType();
	}
	*/
	
	// Required public by gson ???
	public RPCObject( T t, String name ) {
		appdata = t;
		this.name = name;
		hostname = HOSTNAME;
		protocolver = release = VERSION;
		platform = PLATFORM;
		pid = 0;
		time = (int)(System.currentTimeMillis() / 1000);
	}
	
	public final T appdata;
	public final String name, hostname;
	public final String protocolver, release, platform;
	public final int pid, time;
   
	static private String HOSTNAME = "UNKNOWN";
	static {
		try {
			InetAddress ia = InetAddress.getLocalHost();
			HOSTNAME = ia.getCanonicalHostName();
		} catch( UnknownHostException uhe ) {
			// How to report ???
			System.err.println( uhe );
		}
	}

	static private String VERSION = "UNKOWN";
	static {
		Package p = DIMSBase.class.getPackage();
		try {
			VERSION = p.getImplementationVersion();
			// System.err.println( "Version: " + VERSION );
		} catch( Exception e ) {
			// How to report ???
			System.err.println( e );
		}
	}

	static private String PLATFORM = "UNKOWN";
	static {
		try {
			Process p = Runtime.getRuntime().exec( "uname -a" );
			p.waitFor();
			InputStream is = p.getInputStream();
			byte[] bs = new byte[1024];
			int nin = is.read( bs );
			is.close();
			if( nin > 0 ) {
				PLATFORM = new String( bs, 0, nin ).trim();
			}
			// System.err.println( "Platform: " + PLATFORM );
		} catch( Exception e ) {
			// How to report ???
			System.err.println( e );
		}
	}
}

// eof


