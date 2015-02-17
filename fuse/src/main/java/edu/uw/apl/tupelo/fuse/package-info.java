/**
 * Classes enabling the managed disks in a Tupelo store to be made
 * available as device file artifacts.  These 'files' can then be
 * processed by Sleuthkit and other system level tools (<code>md5sum,
 * dd, etc</code>). Uses Fuse4j/Fuse to achieve this.
 *
 * <p>
 *
 * The package contains the main ManagedDiskFileSystem class,
 * responsible for exposing a Store's contents to Fuse.  Also provided
 * is Main, enabling command line invocation.  Other entry points into
 * an 'MDFS' would be as part of a larger Tupelo http server (war).
 */
package edu.uw.apl.tupelo.fuse;
