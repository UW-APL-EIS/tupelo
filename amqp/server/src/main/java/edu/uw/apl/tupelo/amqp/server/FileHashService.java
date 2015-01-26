package edu.uw.apl.tupelo.amqp.server;

import java.lang.reflect.Type;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Base64;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AMQP.BasicProperties;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;
import edu.uw.apl.tupelo.store.Store;

import edu.uw.apl.tupelo.amqp.objects.FileHashQuery;
import edu.uw.apl.tupelo.amqp.objects.FileHashResponse;
import edu.uw.apl.tupelo.amqp.objects.JSONSerializers;
import edu.uw.apl.tupelo.amqp.objects.RPCObject;
import edu.uw.apl.tupelo.amqp.objects.Utils;

/**
 * Connect to an AMQP broker (url supplied) and listen for messages on
 * the 'tupelo' exchange, a direct exchange.  The message set we
 * understand (and used as our binding from queue to exchange) is
 * currently just 'who-has'.  The message payload for messages to that
 * queue are is an instance of {@link FileHashQuery}.  We then attempt
 * to load the 'md5' attribute from all managed disks (WHAT,WHEN) in
 * our configured Tupelo store and search for the needles (hashes in
 * FileHashQuery) in the haystacks (managed disk filesystem content).
 * We then reply over amqp with a {@link FileHashResponse} structure.
 * Both FileHashQuery and FileHashResponse are encoded as JSON, tied
 * to a {@link RPCObject} which supplies debug/metadata.
 *
 * LOOK: Currently we know about a single store only.  Extend to many??
 */

public class FileHashService {

	public FileHashService( Store s, String brokerURL ) {
		store = s;
		this.brokerURL = brokerURL;
		gson = Utils.createGson( true );
		log = LogFactory.getLog( getClass() );
	}
	
	public void start() throws Exception {
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();

		ConnectionFactory cf = new ConnectionFactory();
		cf.setUri( brokerURL );
		Connection connection = cf.newConnection();
		channel = connection.createChannel();

		channel.exchangeDeclare( EXCHANGE, "direct" );
		
		String queueName = channel.queueDeclare().getQueue();
		channel.queueBind( queueName, EXCHANGE, BINDINGKEY );
		log.info( "Binding to exchange '" + EXCHANGE + "' with key '"
				  + BINDINGKEY + "'" );
        QueueingConsumer consumer = new QueueingConsumer(channel);
		boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, consumer);

        while (true) {
			log.info( "Waiting..." );
            QueueingConsumer.Delivery delivery = null;

			try {
				delivery = consumer.nextDelivery();
			} catch( ShutdownSignalException sse ) {
				log.warn( sse );
				break;
			}

			// LOOK: algorithm match
			
			String message = new String( delivery.getBody() );
			// LOOK: check mime type aka contentType
			String json = message;
			
            log.info( "Received request '" + json + "'");

			Type fhqType =
				new TypeToken<RPCObject<FileHashQuery>>(){}.getType();
			RPCObject<FileHashQuery> rpc1 = null;

			try {
				rpc1 = gson.fromJson( json, fhqType );
			} catch( JsonParseException jpe ) {
				log.warn( jpe + " -> " + json );
				continue;
			}
			FileHashQuery fhq = rpc1.appdata;

            log.info( "Searching for " + fhq.hashes.size() + " hashes..." );

			FileHashResponse fhr = new FileHashResponse( fhq.algorithm );
			
			/*
			  LOOK: load all the Store's md5 hash data on every query ??
			  Better to load it once (but then what if stored updated?)
			*/
			for( ManagedDiskDescriptor mdd : mdds ) {
				List<String> ss = loadFileHashes( mdd );
				
				log.info( "Loaded " + ss.size() + " hashes from " + mdd );
				/*
				  Recall: The file content of MANY paths can hash to
				  the SAME value, typically when the file is empty.
				*/
				Map<BigInteger,List<String>> haystack = buildHaystack( ss );

				for( byte[] hash : fhq.hashes ) {
					// 1 means 'this value is positive'
					BigInteger needle = new BigInteger( 1, hash );
					List<String> paths = haystack.get( needle );
					if( paths == null )
						continue;
					String hashHex = new String( Hex.encodeHex( hash ) );
					log.info( "Matched " +  hashHex + ": " + mdd + " "
							  + paths );
					for( String path : paths )
						fhr.add( hash, mdd, path );
				}					
			}

			channel.basicAck( delivery.getEnvelope().getDeliveryTag(), false );
			BasicProperties reqProps = delivery.getProperties();
			BasicProperties resProps = new BasicProperties.Builder()
				.contentType( "application/json" )
				.build();
			RPCObject<FileHashResponse> rpc2 = RPCObject.asRPCObject
				( fhr, "filehash" );
			json = gson.toJson( rpc2 );
			channel.basicPublish( "", reqProps.getReplyTo(),
								  resProps, json.getBytes() );
            log.info( "Sending reply '" + json + "'");
        }
	}

	public void stop() throws IOException {
		if( channel == null )
			return;
		channel.close();
	}
	
	/*
	  Load the 'md5' attribute data from the store for the given
	  ManagedDiskDescriptor.  Just load the whole lines, do NOT parse
	  anything at this point.
	*/
	private List<String> loadFileHashes( ManagedDiskDescriptor mdd )
		throws IOException {
		List<String> result = new ArrayList<String>();
		Collection<String> attrNames = store.listAttributes( mdd );
		for( String attrName : attrNames ) {
			if( !attrName.startsWith( "hashfs-" ) )
				continue;
			byte[] ba = store.getAttribute( mdd, attrName );
			if( ba == null )
				continue;
			ByteArrayInputStream bais = new ByteArrayInputStream( ba );
			InputStreamReader isr = new InputStreamReader( bais );
			LineNumberReader lnr = new LineNumberReader( isr );
			String line = null;
			while( (line = lnr.readLine()) != null ) {
				line = line.trim();
				if( line.isEmpty() || line.startsWith( "#" ) )
					continue;
				result.add( line );
			}
		}
		return result;
	}


	/**
	 * Turn the contents of a manageddisk's md5 attribute, stored as a
	 * text file of the form
	 *
	 * hashHex /path/to/file
	 * hashHex /path/to/other/file
	 *
	 * into a map of hash (as BigInteger) -> List<String>
	 *
	 * Then, given an hash as needle, we can locate ALL files with that hash
	 */
	private Map<BigInteger,List<String>> buildHaystack( List<String> ss ) {
		Map<BigInteger,List<String>> result =
			new HashMap<BigInteger,List<String>>();
		for( String s : ss ) {
			String[] toks = s.split( "\\s+", 2 );
			if( toks.length < 2 ) {
				log.warn( s );
				continue;
			}
			String md5Hex = toks[0];
			String path = toks[1];
			byte[] md5 = null;
			try {
				md5 = Hex.decodeHex( md5Hex.toCharArray() );
			} catch( DecoderException de ) {
				log.warn( de );
				continue;
			}
			// 1 means 'this value is positive'
			BigInteger bi = new BigInteger( 1, md5 );
			List<String> paths = result.get( bi );
			if( paths == null ) {
				paths = new ArrayList<String>();
				result.put( bi, paths );
			}
			paths.add( path );
		}
		return result;
	}

	private final Store store;
	private final String brokerURL;
	private Channel channel;
	private Gson gson;
	private Log log;
	
	static final String EXCHANGE = "tupelo";

	static final String BINDINGKEY = "who-has";
}

// eof
