/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * http://en.wikipedia.org/wiki/SCSI_Inquiry_Command
 *
 * also sg_inq -p 0x80 /device (sg3utils) shows that length of serial
 * num string is in response[3], with the string starting in
 * response[4]
 */

#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <linux/fs.h>
#include <sys/ioctl.h>
#include <scsi/sg.h> /* take care: fetches glibc's /usr/include/scsi/sg.h */


int64_t diskSize( const char* pathName ) {

  if( !pathName )
	return -1;

  int fd = open( pathName, O_RDONLY );
  if( fd == -1 ) {
	return -1;
  }

  // derive size...
  int64_t size = 0;
  int sc = ioctl(fd, BLKGETSIZE64, &size);
  close( fd );
  if( sc == -1 ) {
	return -1;
  }
  return size;
}

#define INQ_REPLY_LEN 96
#define INQ_CMD_CODE 0x12
#define INQ_CMD_LEN 6

int scsiInquiry( const char* pathName, 
				 char vendorIDResult[],
				 char productIDResult[], 
				 char serialNumberResult[] ) {

  int fd = open( pathName, O_RDONLY );
  if( fd == -1 ) {
	/* Note that most SCSI commands require the O_RDWR flag to be set */
	return -1;
  }

  int k;
  unsigned char inqCmdBlk[INQ_CMD_LEN] =
	{INQ_CMD_CODE, 0, 0, 0, INQ_REPLY_LEN, 0};
  //	{INQ_CMD_CODE, 1, 0x80, 0, INQ_REPLY_LEN, 0};
  /* This is a "standard" SCSI INQUIRY command. It is standard because the
   * CMDDT and EVPD bits (in the second byte) are zero. All SCSI targets
   * should respond promptly to a standard INQUIRY */
  unsigned char inqBuff[INQ_REPLY_LEN];
  unsigned char sense_buffer[32];
  sg_io_hdr_t io_hdr;
  
  /* It is prudent to check we have a sg device by trying an ioctl */
  if ((ioctl(fd, SG_GET_VERSION_NUM, &k) < 0) || (k < 30000)) {
	printf("%d is not an sg device, or old sg driver\n", fd);
	close( fd );
	return 1;
  }
  
  /* Prepare INQUIRY command */
  memset(&io_hdr, 0, sizeof(sg_io_hdr_t));
  io_hdr.interface_id = 'S';
  io_hdr.cmd_len = sizeof(inqCmdBlk);
  /* io_hdr.iovec_count = 0; */  /* memset takes care of this */
  io_hdr.mx_sb_len = sizeof(sense_buffer);
  io_hdr.dxfer_direction = SG_DXFER_FROM_DEV;
  io_hdr.dxfer_len = INQ_REPLY_LEN;
  io_hdr.dxferp = inqBuff;
  io_hdr.cmdp = inqCmdBlk;
  io_hdr.sbp = sense_buffer;
  io_hdr.timeout = 20000;     /* 20000 millisecs == 20 seconds */
  /* io_hdr.flags = 0; */     /* take defaults: indirect IO, etc */
  /* io_hdr.pack_id = 0; */
  /* io_hdr.usr_ptr = NULL; */
  
  if (ioctl(fd, SG_IO, &io_hdr) < 0) {
	perror("sg_simple0: Inquiry SG_IO ioctl error");
	close( fd );
	return 1;
  }
  
  /* now for the error processing */
  if ((io_hdr.info & SG_INFO_OK_MASK) != SG_INFO_OK) {
	if (io_hdr.sb_len_wr > 0) {
	  printf("INQUIRY sense data: ");
	  for (k = 0; k < io_hdr.sb_len_wr; ++k) {
		if ((k > 0) && (0 == (k % 10)))
		  printf("\n  ");
		printf("0x%02x ", sense_buffer[k]);
	  }
	  printf("\n");
	}
	if (io_hdr.masked_status)
	  printf("INQUIRY SCSI status=0x%x\n", io_hdr.status);
	if (io_hdr.host_status)
	  printf("INQUIRY host_status=0x%x\n", io_hdr.host_status);
	if (io_hdr.driver_status)
	  printf("INQUIRY driver_status=0x%x\n", io_hdr.driver_status);
  } else {  /* assume INQUIRY response is present */
	char * p = (char *)inqBuff;
	strncpy( vendorIDResult, p+8, 8 );
	strncpy( productIDResult, p+16, 16 );
  }
  
  unsigned char inqCmdBlk2[INQ_CMD_LEN] =
	{INQ_CMD_CODE, 1, 0x80, 0, INQ_REPLY_LEN, 0};
  /* Prepare INQUIRY command */
  memset(&io_hdr, 0, sizeof(sg_io_hdr_t));
  io_hdr.interface_id = 'S';
  io_hdr.cmd_len = sizeof(inqCmdBlk);
  /* io_hdr.iovec_count = 0; */  /* memset takes care of this */
  io_hdr.mx_sb_len = sizeof(sense_buffer);
  io_hdr.dxfer_direction = SG_DXFER_FROM_DEV;
  io_hdr.dxfer_len = INQ_REPLY_LEN;
  io_hdr.dxferp = inqBuff;
  io_hdr.cmdp = inqCmdBlk2;
  io_hdr.sbp = sense_buffer;
  io_hdr.timeout = 20000;     /* 20000 millisecs == 20 seconds */
  /* io_hdr.flags = 0; */     /* take defaults: indirect IO, etc */
  /* io_hdr.pack_id = 0; */
  /* io_hdr.usr_ptr = NULL; */
  
  if (ioctl(fd, SG_IO, &io_hdr) < 0) {
	perror("sg_simple0: Inquiry SG_IO ioctl error");
	close( fd );
	return 1;
  }
  
  /* now for the error processing */
  if ((io_hdr.info & SG_INFO_OK_MASK) != SG_INFO_OK) {
	if (io_hdr.sb_len_wr > 0) {
	  printf("INQUIRY sense data: ");
	  for (k = 0; k < io_hdr.sb_len_wr; ++k) {
		if ((k > 0) && (0 == (k % 10)))
		  printf("\n  ");
		printf("0x%02x ", sense_buffer[k]);
	  }
	  printf("\n");
	}
	if (io_hdr.masked_status)
	  printf("INQUIRY SCSI status=0x%x\n", io_hdr.status);
	if (io_hdr.host_status)
	  printf("INQUIRY host_status=0x%x\n", io_hdr.host_status);
	if (io_hdr.driver_status)
	  printf("INQUIRY driver_status=0x%x\n", io_hdr.driver_status);
  } else {  /* assume INQUIRY response is present */
	char * p = (char *)inqBuff;
	int len = p[3] & 0xff;
	strncpy( serialNumberResult, p+4, len );
  }
  
  close( fd );
  return 0;
}

// eof
