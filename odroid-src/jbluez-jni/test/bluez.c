/*
 * Test application for "btj", the bluetooth interface
 * for the "Bert" robot.
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
			else if( strcmp(argv[1],"device")) local_device_id();
		}
	}
	return 0;
}

int local_device_id() {
	/* Find the HCI device ID for a local device with the given BT address */
	/* Returns the HCI device ID as an int.                                */
	char *bdaddr_str = "";
	int devID = 0;

	return devID;
}


void usage() {
	printf("Usage:\n");
	printf("\t-h\t-help, print this text\n");
	printf("\t-l\t-local, apply to local device (default)\n");
	printf("\t-l\t-remote, apply to remote device(s)\n");
	printf("\t-device\t-show the device id(s)\n");
}
