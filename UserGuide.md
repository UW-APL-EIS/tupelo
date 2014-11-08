Tupelo User Guide
=================

Started by Stuart, Nov 2014.

Tupelo is a 'whole disk management system'.  This just means that the
unit of currency in Tupelo is a whole disk, rather than a file or
filesystem.  Tupelo is written mostly in Java, with some
platform-specific parts in C.  We view Tupelo not as 'a program' but
rather as a code base from which various runtime components can be
built (and some have been!)

Central to Tupelo is the 'store', which is just a database where disk
contents are stored!  Our first store implementation is called a 'file
system store' and uses, as you can probably guess, a directory
structure under a root directory to hold the disks. When we say
database, we do NOT mean Mysql or any form of RDBMS, we just mean a
mechanism for storing and later accessing chunks of disk data.

You 'put' disks into the store.  You start with 'unmanaged data', just
like in git you start with untracked files.  You put an unmanaged data
(whole disk contents) into a store, and now that data is 'managed',
just like how in git you add a file and it becomes tracked.  One major
difference is that in Tupelo a managed disk can never change.

A managed disk is identified by a pair that represent WHAT and WHEN.
The WHAT comes from a (hopefully) globally unique identifier for the
disk you want Tupelo to manage.  We call this the 'disk id'.  There
are three types of 'disk' that Tupelo can handle:

* Physical Disks.  A whole hard drive, such as C: or /dev/sda.  Tupelo
  interrogates the disk at a low level (using e.g. SCSI/ATA commands)
  to extract from the disk its vendor, serial number. etc.  An example
  is 'ATA/SAMSUNG HD253GJ/S26CJ90B203913'.  It is hoped that this disk
  and ONLY this disk would ever exhibit that identifying string.

* Disk Images.  A file on disk that represents some whole or part of a
  disk.  For these disk type, we just use the file name for our
  diskID, conceding that this is hardly globally unique.  Disk Images
  are really only intended for testing/experimentation in Tupelo.

* Virtual Disks.  The whole disk contents of a virtual machine under a
  VM Manager such as VirtualBox.  A VM's 'hard drive' is actually just
  a file (or set of files) in the host system.  The .vdi or .vmdk
  files in your VirtualBox vm workspace are virtual disks.  Tupelo has
  code that can 'see inside' these .vdi/.vmdk files to get at the
  contents of the hard drive as seen by the VM (Note: not fully done
  yet!)

The WHEN comes from the notion of a 'session' that is handled by a
Tupelo store.  When you 'connect' to a Tupelo store, it hands you a
session object, kind of like a ticket.  The session roughly identifies
a moment in time.  A store generates a session thus '2nd session for
Nov 7 2014', etc.  When you 'put' a disk into a Tupelo store you are
saying 'this is the content of this disk right now', where, via the
session, the store is actually deciding what 'now' is.  If you later
connect to the same store, you would get a new session, so you could
never overwrite the contents of a managed disk in the store with new
content.

We call the WHAT,WHEN pair associated with a managed disk the 'managed
disk descriptor'.

To test putting an unmanaged disk into a Tupelo store, you first need
to obtain and build the Tupelo codebase.  Check out the tupelo.git
repo (and see ./README.md for build pre-requisites). All features of
Tupelo have been tested on Linux 32 and 64 bit systems.  On other
platforms (Windows,MacOS) some parts may not work, since in a few
places Tupelo does use native C code and I have yet to build those on
Windows, MacOS.  The 'put' operation and basic store content
navigation should work on all platforms.

$ cd /path/to/tupelo
$ mvn clean package

If this works, great! We now some Tupelo programs we can run.  Now cd
to the module containing some command line tools:

$ cd cli

Next, create a disk image from scratch to use as unmanaged data.  On
Linux (and MacOS?) at least

$ dd if=/dev/sda of=DISKIMAGE bs=1M count=4096

will copy the first 4GB of your actual hard drive into a local file
./DISKIMAGE.  Of course this is NOT a valid filesystem, since we have
cropped/truncated the disk at 4GB!  To create a disk image in which
there is a valid file system, and then add some local file into that
filesystem, we can create a 'filesystem in a file', like this (on
Linux/MacOS??)

$ dd if=/dev/zero of=FILESYSTEM bs=1M count=4096
$ losetup -f
$ losetup /dev/loop0 FILESYSTEM
$ losetup -a
$ mkfs.ext3 /dev/loop0
$ mkdir mount
$ sudo mount /dev/loop0 mount
$ mkdir mount/bin
$ cp /bin/* mount/bin
$ sync
$ sudo umount /dev/loop0
$ losetup -d /dev/loop0
$ losetup -a

As far as simply transferring a local 'unmanaged disk' to become
'managed' in a Tupelo store, either one of DISK and FILESYSTEM are OK.
 
Next, we create a 'store root' directory, since Tupelo won't auto-create it:

$ mkdir test-store

This name 'test-store' is the default in the programs described next
so makes their usage a bit easier.  First thing, let's inspect our new
Tupelo store:

$ ./storeinfo

should print out how much space the store has got, which is all the
disk space available under the store root directory.  To put an
unmanaged disk into this store:

$ ./putdata FILESYSTEM (or DISK)

Inspect the store again and it should show you the WHAT, WHEN of the
disk just submitted.

$ ./storeinfo


Tupelo has some smart compression logic to minimise the amount of disk
space required to hold the submitted disk.  That's why the sizes of
unmanaged and managed disks are likely different.  The putdata program
decides to use a 'flat disk' if the unmanaged size is < 1GB, or a
'stream optimized disk' otherwise.  A flat disk form of managed disk
is simply the original unmanaged disk with a small (512 byte) header
on the front.  So a flat disk is actually bigger than its unmanaged
counterpart!  A stream-optimized disk is much smarter!  We can force
either variation explicitly (though you would never really want to do
this??)

$ ./putdata -f FILESYSTEM

$ ./putdata -o FILESYSTEM

MORE COMING

