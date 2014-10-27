package edu.uw.apl.tupelo.amqp.server;

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

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Hex;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.filesys.FilesystemStore;
import edu.uw.apl.tupelo.http.client.HttpStoreProxy;

public class AMQPServer {

	static public void main( String[] args ) throws Exception {
		AMQPServer main = new AMQPServer();
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

	AMQPServer() {
		storeLocation = STORELOCATIONDEFAULT;
		brokerUrl = BROKERURLDEFAULT;
		log = Logger.getLogger( getClass() );
	}

	public void readArgs( String[] args ) {
		Options os = new Options();
		os.addOption( "d", false, "Debug" );
		os.addOption( "v", false, "Verbose" );
		os.addOption( "s", true,
					  "Store url/directory. Defaults to " +
					  STORELOCATIONDEFAULT );
		os.addOption( "u", true,
					  "Broker url. Defaults to " +
					  BROKERURLDEFAULT );

		final String USAGE =
			AMQPServer.class.getName() +
			" [-d] [-v] [-s storeLocation] [-u brokerURL]";
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
		debug = cl.hasOption( "d" );
		verbose = cl.hasOption( "v" );
		if( cl.hasOption( "s" ) ) {
			storeLocation = cl.getOptionValue( "s" );
		}
		if( cl.hasOption( "u" ) ) {
			brokerUrl = cl.getOptionValue( "u" );
		}
		args = cl.getArgs();
	}

	public void start() throws Exception {
		store = buildStore( storeLocation );
		Collection<ManagedDiskDescriptor> mdds = store.enumerate();
		
		ConnectionFactory cf = new ConnectionFactory();
		cf.setUri( brokerUrl );
		Connection connection = cf.newConnection();
		Channel channel = connection.createChannel();

		channel.exchangeDeclare( EXCHANGE, "direct" );
		
		String queueName = channel.queueDeclare().getQueue();
		channel.queueBind( queueName, EXCHANGE, "who-has" );

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();

			BasicProperties reqProps = delivery.getProperties();
			String reqContentType = reqProps.getContentType();

			BasicProperties resProps = new BasicProperties.Builder()
				.build();
			String message = new String(delivery.getBody());

            System.out.println(" [x] Received '" + message + "'");

			for( ManagedDiskDescriptor mdd : mdds ) {
				List<String> ss = loadFileHashes( mdd );
				System.out.println( ss.size() );
				Map<BigInteger,String> haystack = new HashMap<BigInteger,String>();
				for( String s : ss ) {
					String[] toks = s.split( "\\s+" );
					if( toks.length < 2 ) {
						log.warn( s );
						continue;
					}
					String md5Hex = toks[0];
					String path = toks[1];
					byte[] md5 = Hex.decodeHex( md5Hex.toCharArray() );
					BigInteger bi = new BigInteger( 1, md5 );
					haystack.put( bi, path );
				}
			}
			
			String msg = "world";
			channel.basicPublish( "", reqProps.getReplyTo(),
								  resProps, msg.getBytes() );
            System.out.println(" [x] Sent '" + msg + "'");
			
        }
	}
	
	static public Store buildStore( String storeLocation ) {
		Store s = null;
		if( storeLocation.startsWith( "http" ) ) {
			s = new HttpStoreProxy( storeLocation );
		} else {
			File dir = new File( storeLocation );
			if( !dir.isDirectory() ) {
				throw new IllegalStateException
					( "Not a directory: " + storeLocation );
			}
			s = new FilesystemStore( dir );
		}
		return s;
	}

	/*
	  Load the 'md5' attribute data from the store for the given
	  ManagedDiskDescriptor.  Just load the whole lines, do NOT parse
	  anything at this point.
	*/
	private List<String> loadFileHashes( ManagedDiskDescriptor mdd )
		throws IOException {
		byte[] ba = store.getAttribute( mdd, "md5" );
		if( ba == null )
			return Collections.emptyList();
		List<String> result = new ArrayList<String>();
		ByteArrayInputStream bais = new ByteArrayInputStream( ba );
		InputStreamReader isr = new InputStreamReader( bais );
		LineNumberReader lnr = new LineNumberReader( isr );
		String line = null;
		while( (line = lnr.readLine()) != null ) {
			line = line.trim();
			if( line.isEmpty() || line.startsWith( "#") )
				continue;
			result.add( line );
		}
		return result;
	}
	
	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	private String storeLocation;
	private Store store;
	private Logger log;
	
	private String brokerUrl;
	static boolean debug, verbose;
	
	static final String STORELOCATIONDEFAULT = "./test-store";

	static final String BROKERURLDEFAULT =
		"amqp://rpc_user:rpcm3pwd@rabbitmq.prisem.washington.edu";
	
	static final String EXCHANGE = "tupelo";
}

// eof
