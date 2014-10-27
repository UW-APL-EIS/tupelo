package edu.uw.apl.tupelo.amqp.client;

import org.apache.commons.cli.*;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;

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
			.setContentType( "text/plain" );
			.build();

		String msg = "hello";
		channel.basicPublish( EXCHANGE, "who-has", bp, msg.getBytes() );
		

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume( replyQueueName, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            System.out.println(" [x] Received '" + message + "'");
        }
	}
	
	static private void printUsage( Options os, String usage,
									String header, String footer ) {
		HelpFormatter hf = new HelpFormatter();
		hf.setWidth( 80 );
		hf.printHelp( usage, header, os, footer );
	}

	private String brokerUrl;
	static boolean debug, verbose;
	
	static final String BROKERURLDEFAULT =
		"amqp://rpc_user:rpcm3pwd@rabbitmq.prisem.washington.edu";
	
	static final String EXCHANGE = "tupelo";
}

// eof
