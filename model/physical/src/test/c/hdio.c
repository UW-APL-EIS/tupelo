#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <linux/hdreg.h>

/**
   discover device (disk) parameters via the HDIO_GET_IDENTITY ioctl,
   as used by e.g hdparm.

   By inspection of result buf, looks like

   buf[20-33] = serial num (14), may be whitespace-padded internally.
   buf[54-68] = manufacturer, model (what scsi INQUIRY calls 'VendorID'??)

   Note: fails on a usb external drive. scsi INQUIRY worked on this device,
   so might be a better choice since works with more drive types??
*/

int main( int argc, char* argv[] ) {

    char *device = "/dev/sda";
	if( argc > 1 )
	  device = argv[1];

	
	int fd = open( device, O_RDONLY);
  if( fd == -1 ) {
	perror("open");
	return -1;
  }

  char identity[512];
  int sc = ioctl( fd, HDIO_GET_IDENTITY, identity );
  if( sc == -1 ) {
	perror( "ioctl" );
	close( fd );
	return -1;
  }
  int i;
  for( i = 0; i < sizeof( identity ); i++ ) {
	if( identity[i] ) {
	  printf( "%d %c (%x)\n", i , identity[i], identity[i] & 0xff );
	}
  }
	
}

// eof

