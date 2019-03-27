/*
 * Test application for "btj", the bluetooth interface
 * for the "Bert" robot.
 */

#include <stdio.h>

int main(int argc, char* argv[]) {
	// Read through command-line arguments for options.
	while(--argc) {
		printf("%s\n",*(argv++));
	}
}
