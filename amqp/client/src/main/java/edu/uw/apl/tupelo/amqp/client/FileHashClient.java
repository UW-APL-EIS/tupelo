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
package edu.uw.apl.tupelo.amqp.client;

import java.lang.reflect.Type;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Hex;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

import edu.uw.apl.tupelo.amqp.objects.FileHashQuery;
import edu.uw.apl.tupelo.amqp.objects.FileHashResponse;
import edu.uw.apl.tupelo.amqp.objects.RPCObject;
import edu.uw.apl.tupelo.amqp.objects.Utils;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.utils.Discovery;

/**
 * Put a 'who-has file content matching this hash' request on to a
 * RabbitMQ message bus.  Both the request and response(s) are
 * JSON-formatted.  See the 'objects' module (sibling to this client)
 * for definitions of the JSON encodings.  In short, the FileHashQuery
 * is a pair of key/values, e.g.
 *
 * { "algorithm" : "md5", "hashes" : ["hash1hex", "hash2hex" ] }
 *
 * The response object is a FileHashResponse. Both FileHashQuery and
 * FileHashResponse are encoded as JSON, tied to a {@link RPCObject}
 * which supplies debug/metadata.
 *
 * The file hashes (assumed to be md5 hashes, LOOK extend to handle
 * e.g. sha1) are read from
 *
 * (a) cmd line arguments.  Use xargs to convert a text file of hashes
 * to cmd line args, e.g. cat FILE | xargs filehashclient
 *
 * (b) stdin, if no cmd line arguments found. Use directly, e.g.
 *
 * $ filehashclient < FILE
 * $ cat FILE | filehashclient
 * $ echo HASH | filehashclient
 *
 * @see FileHashQuery
 * @see FileHashResponse
 * @see RPCObject
 */
public class FileHashClient {

	static public void main( String[] args ) throws Exception {
		FileHashClient main = new FileHashClient();
		try {
			main.readArgs( args );
			main.start();
		} catch( Exception e ) {
			System.err.println( e );
			if( debug )
				e.printStackTrace();
			System.exit(-1);
		}
	}

	FileHashClient() {
		log = Logger.getLogger( getClass() );
		boolean withPrettyPrinting = true;
		gson = Utils.createGson( withPrettyPrinting );
		hashes = new ArrayList<String>();
		brokerUrl = Discovery.locatePropertyValue( "amqp.url" );
		log.info( "BrokerUrl: " + brokerUrl );
	}

	
	public void readArgs( String[] args ) throws IOException {
		Options os = new Options();
		os.addOption( "d", false, "Debug" );
		os.addOption( "v", false, "Verbose" );
		os.addOption( "u", true,
					  "Broker url. Can also be located on path and in resource" );
		os.addOption( "V", false, "show version number and exit" );

		final String USAGE =
			FileHashClient.class.getName() + " [-d] [-v] [-u brokerURL] [-V] hashstring+";
		final String HEADER = "";
		final String FOOTER = "";
		
		CommandLineParser clp = new PosixParser();
		CommandLine cl = null;
		try {
			cl = clp.parse( os, args );
		} catch( ParseException pe ) {
			printUsage( os, USAGE, HEADER, FOOTER );
			System.exit(1);
		}
		if( cl.hasOption( "V" ) ) {
			Package p = getClass().getPackage();
			String version = p.getImplementationVersion();
			System.out.println( p.getName() + "/" + version );
			System.exit(0);
		}
		debug = cl.hasOption( "d" );
		verbose = cl.hasOption( "v" );
		if( cl.hasOption( "u" ) ) {
			brokerUrl = cl.getOptionValue( "u" );
		}
		args = cl.getArgs();
		if( args.length > 0 ) {
			// hashes are in cmd line args
			for( String arg : args ) {
				arg = arg.trim();
				if( arg.isEmpty() || arg.startsWith( "#" ) )
					continue;
				hashes.add( arg );
			}
		} else {
			// hashes are on stdin
			InputStreamReader isr = new InputStreamReader( System.in );
			BufferedReader br = new BufferedReader( isr );
			String line;
			while( (line = br.readLine()) != null ) {
				line = line.trim();
				if( line.isEmpty() || line.startsWith( "#" ) )
					continue;
				hashes.add( line );
			}
			br.close();
		}
	}

	public void start() throws Exception {
		ConnectionFactory cf = new ConnectionFactory();
		cf.setUri( brokerUrl );
		Connection connection = cf.newConnection();
		Channel channel = connection.createChannel();

		channel.exchangeDeclare( EXCHANGE, "direct" );
		
		String replyQueueName = channel.queueDeclare().getQueue();

		BasicProperties bp = new BasicProperties.Builder()
			.replyTo( replyQueueName )
			.contentType( "application/json" )
			.correlationId( "" + System.currentTimeMillis()  )
			.build();

		// LOOK: populate the fhq via add( byte[] )
		FileHashQuery fhq = new FileHashQuery( "md5" );
		for( String hash : hashes ) {
			char[] cs = hash.toCharArray();
			byte[] bs = Hex.decodeHex( cs );
			fhq.add( bs );
		}
		RPCObject<FileHashQuery> rpc1 = RPCObject.asRPCObject( fhq,
															   "filehash" );
		String json = gson.toJson( rpc1 );
		log.info( "Sending request '" + json + "'" );
		
		channel.basicPublish( EXCHANGE, "who-has", bp, json.getBytes() );

        QueueingConsumer consumer = new QueueingConsumer(channel);
		boolean autoAck = true;
        channel.basicConsume( replyQueueName, autoAck, consumer);

		QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		String message = new String(delivery.getBody());
		
		// look: check contentType
		json = message;
		log.info( "Received reply '" + json + "'" );

		Type fhrType = new TypeToken<RPCObject<FileHashResponse>>(){}.getType();
		RPCObject<FileHashResponse> rpc2 = gson.fromJson( json, fhrType );
		FileHashResponse fhr = rpc2.appdata;
		for( FileHashResponse.Hit h : fhr.hits ) {
			String hashHex = new String( Hex.encodeHex( h.hash ) );
			System.out.println( "Hit: " + hashHex + " " + h.descriptor + " " +
								h.path );
		}

		channel.close();
		connection.close();
	}
	
	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	private String brokerUrl;
	private Gson gson;
	private Logger log;
	
	static boolean debug, verbose;
	
	private List<String> hashes;
	
	static final String EXCHANGE = "tupelo";
}

// eof
