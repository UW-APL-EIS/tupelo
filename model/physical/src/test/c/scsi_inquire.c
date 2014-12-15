/**
   http://stackoverflow.com/questions/2432759/usb-drive-serial-number-under-linux-c
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <scsi/scsi.h>
#include <scsi/sg.h>
#include <sys/ioctl.h>

int scsi_get_serial(int fd, void *buf, size_t buf_len) {
    // we shall retrieve page 0x80 as per http://en.wikipedia.org/wiki/SCSI_Inquiry_Command
    unsigned char inq_cmd[] = {INQUIRY, 1, 0x80, 0, buf_len, 0};
    unsigned char sense[32];
    struct sg_io_hdr io_hdr;
            int result;

    memset(&io_hdr, 0, sizeof (io_hdr));
    io_hdr.interface_id = 'S';
    io_hdr.cmdp = inq_cmd;
    io_hdr.cmd_len = sizeof (inq_cmd);
    io_hdr.dxferp = buf;
    io_hdr.dxfer_len = buf_len;
    io_hdr.dxfer_direction = SG_DXFER_FROM_DEV;
    io_hdr.sbp = sense;
    io_hdr.mx_sb_len = sizeof (sense);
    io_hdr.timeout = 5000;

    result = ioctl(fd, SG_IO, &io_hdr);
    if (result < 0)
        return result;

    if ((io_hdr.info & SG_INFO_OK_MASK) != SG_INFO_OK)
        return 1;

    return 0;
}


int main(int argc, char** argv) {
    char *dev = "/dev/sda";

	if( argc > 1 )
	  dev = argv[1];

    char scsi_serial[255];
    int rc;
    int fd;

    fd = open(dev, O_RDONLY | O_NONBLOCK);
    if (fd < 0) {
        perror(dev);
    }

    memset(scsi_serial, 0, sizeof (scsi_serial));
    rc = scsi_get_serial(fd, scsi_serial, 255);
    // scsi_serial[3] is the length of the serial number
    // scsi_serial[4] is serial number (raw, NOT null terminated)
    if (rc < 0) {
        printf("FAIL, rc=%d, errno=%d\n", rc, errno);
    } else
    if (rc == 1) {
        printf("FAIL, rc=%d, drive doesn't report serial number\n", rc);
    } else {
        if (!scsi_serial[3]) {
            printf("Failed to retrieve serial for %s\n", dev);
            return -1;
        }
        printf("Serial Number: %.*s\n", (size_t) scsi_serial[3], (char *) & scsi_serial[4]);
    }
    close(fd);

    return (EXIT_SUCCESS);
}

// eof
