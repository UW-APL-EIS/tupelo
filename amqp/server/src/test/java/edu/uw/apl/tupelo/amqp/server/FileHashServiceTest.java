package edu.uw.apl.tupelo.amqp.server;

import edu.uw.apl.tupelo.store.Store;
import edu.uw.apl.tupelo.store.null_.NullStore;

public class FileHashServiceTest extends junit.framework.TestCase {

	public void testNull() {
	}

	/**
	 * Start a FileHashService in a background thread T.  Then stop
	 * that service from the main thread and join on T.  Testing that
	 * we can indeed stop/cancel a FileHashService cleanly once it is
	 * in operation.  We will need this teardown ability once we put a
	 * FileHashService component into a web-based store.

	 * LOOK: we are not asserting anything!
	 */
	public void test1() throws Exception {
		Store s = new NullStore();
		String url = BROKERURLDEFAULT;
		final FileHashService fhs = new FileHashService( s, url );
		Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						fhs.start();
					} catch( Exception e ) {
						e.printStackTrace();
					}
				}
			};
		Thread t = new Thread( r );
		t.start();

		Thread.sleep( 4 * 1000L );
		fhs.stop();
		t.join();
	}

	static final String BROKERURLDEFAULT =
		"amqp://rpc_user:rpcm3pwd@rabbitmq.prisem.washington.edu";
}

// eof
