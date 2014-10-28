package edu.uw.apl.tupelo.amqp.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Hex;
import com.google.gson.*;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

import edu.uw.apl.tupelo.amqp.objects.FileHashQuery;
import edu.uw.apl.tupelo.amqp.objects.FileHashResponse;
import edu.uw.apl.tupelo.amqp.objects.JSONSerializers;
import edu.uw.apl.tupelo.model.ManagedDiskDescriptor;
import edu.uw.apl.tupelo.model.Session;

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
		brokerUrl = BROKERURLDEFAULT;
		GsonBuilder gb = new GsonBuilder();
		gb.disableHtmlEscaping();
		gb.registerTypeAdapter(Session.class,
							   new JSONSerializers.SessionSerializer() );
		gb.registerTypeAdapter(byte[].class,
							   new JSONSerializers.MessageDigestSerializer() );
		gson = gb.create();
		hashes = new ArrayList<String>();
	}

	public void readArgs( String[] args ) {
		Options os = new Options();
		os.addOption( "d", false, "Debug" );
		os.addOption( "v", false, "Verbose" );
		os.addOption( "u", true,
					  "Broker url. Defaults to " +
					  BROKERURLDEFAULT );

		final String USAGE =
			FileHashClient.class.getName() +
			" [-d] [-v] [-u brokerURL]";
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
		if( cl.hasOption( "u" ) ) {
			brokerUrl = cl.getOptionValue( "u" );
		}
		args = cl.getArgs();
		for( String arg : args ) {
			arg = arg.trim();
			if( arg.isEmpty() || arg.startsWith( "#" ) )
				continue;
			hashes.add( arg );
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
			.build();

		// LOOK: populate the fhq via add( byte[] )
		FileHashQuery fhq = new FileHashQuery( "md5" );
		for( String hash : hashes ) {
			char[] cs = hash.toCharArray();
			byte[] bs = Hex.decodeHex( cs );
			fhq.add( bs );
		}
		String json = gson.toJson( fhq );
		channel.basicPublish( EXCHANGE, "who-has", bp, json.getBytes() );

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume( replyQueueName, true, consumer);

		QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		String message = new String(delivery.getBody());
		System.out.println(" [x] Received '" + message + "'");
		System.out.println();
		
		// look: check contentType
		json = message;
		FileHashResponse fhr = (FileHashResponse)gson.fromJson
			( json, FileHashResponse.class );
		for( FileHashResponse.Hit h : fhr.hits ) {
			System.out.println( h.path );
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
	static boolean debug, verbose;
	private Gson gson;
	
	private List<String> hashes;
	
	static final String BROKERURLDEFAULT =
		"amqp://rpc_user:rpcm3pwd@rabbitmq.prisem.washington.edu";
	
	static final String EXCHANGE = "tupelo";
}

// eof
