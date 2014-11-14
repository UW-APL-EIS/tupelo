import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.input.CountingInputStream;

/**
 */
public class CountingInputStreamTest extends junit.framework.TestCase {

	public void testNull() {
	}

	public void testAddToEOF() {
		File f = new File( "/dev/null" );
		if( !f.canRead() )
			return;
		try {
			FileInputStream fis = new FileInputStream( f );
			CountingInputStream cis = new CountingInputStream( fis );
			byte[] ba = new byte[16];
			int nin = cis.read( ba );
			long skipped = cis.skip( 10 );
			assertEquals( 16, cis.getByteCount() );
			cis.close();
			fis.close();
		} catch( IOException ioe ) {
			fail( "" + ioe );
		}
	}
}

// eof
