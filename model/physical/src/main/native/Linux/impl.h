#include <stdint.h>

int64_t diskSize( const char* pathName );

int scsiInquiry( const char* pathName, 
		 char vendorIDResult[],
		 char productIDResult[], 
		 char serialNumberResult[] );

//int hwAddr( char* interface, char result[] );

//int ipAddr( char* interface, char result[] );

// eof

