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
		Package p = RPCObject.class.getPackage();
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


