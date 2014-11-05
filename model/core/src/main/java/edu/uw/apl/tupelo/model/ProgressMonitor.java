package edu.uw.apl.tupelo.model;

import java.io.FilterInputStream;

/**
 * Used for monitoring how ManagedDisk (and Store) operations are
 * proceeding.  A way to print the familiar "X % finished" to the
 * user, keeping them informed of possibly lengthy operations.
 */
public interface ProgressMonitor {

	/**
	 * @param percent - e.g. how much of e.g. a ManagedDisk has been
	 * 'written to' a store (whatever written to means for that store)
	 *
	 * @param elapsed - seconds since the operation started, e.g. a
	 * store put
	 */
	public void percentComplete( int percent, long elapsed );

}

// eof
