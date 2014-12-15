#include <stdlib.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <linux/hdreg.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
//#include <cctype>
#include <unistd.h>


int main( int argc, char* argv[] ){
    struct hd_driveid *id;
    char *dev = "/dev/hda";
    int fd;
	
	if( argc > 1 )
	  dev = argv[1];

    fd = open(dev, O_RDONLY|O_NONBLOCK);
    if(fd < 0) {
        perror("cannot open");
    }
    if (ioctl(fd, HDIO_GET_IDENTITY, id) < 0) {
        close(fd);
        perror("ioctl error");
    } else {
        // if we want to retrieve only for removable drives use this branching
        if ((id->config & (1 << 7)) || (id->command_set_1 & 4)) {
            close(fd);
            printf("Serial Number: %s\n", id->serial_no);
        } else {
            perror("support not removable");
        }
        close(fd);
    }
}
