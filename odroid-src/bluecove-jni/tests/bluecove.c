/*
 * Test application for the bluetooth interface.
 */

#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <strings.h>

int local_device_id();
void usage();

bool is_local = true;

int main(int argc, char* argv[]) {
	// Read through command-line arguments for options.
	while(--argc) {
		printf("%s\n",*(argv++));
		if(**argv=='-') {
			printf("%s\n",argv[1]);
			if( strcmp(argv[1],"h")) usage();
			else if( strcmp(argv[1],"l")) is_local = true;
			else if( strcmp(argv[1],"r")) is_local = false;
		}
	}
	return 0;
}


void usage() {
	printf("Usage:\n");
	printf("\t-h\t-help, print this text\n");
	printf("\t-l\t-local, apply to local device (default)\n");
	printf("\t-l\t-remote, apply to remote device(s)\n");
	printf("\t-device\t-show the device id(s)\n");
}
