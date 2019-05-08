/*
 * "blueserver" is an interactive equivalent of "blueserverd". Instead
 * of interfacing with sockets, this reads/writes to stdin/stdout.
 *
 * See "Bluetooth Essentials for Programmers" by Albert Huang
 */

#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <strings.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>
#include "blueserver.h"

#define STDIN 0

int main(int argc, char* argv[]) {
	// The only command-line argument is the port number/
	while(--argc) {
		printf("%s\n",*(argv++));
		if(**argv=='-') {
			printf("%s\n",argv[1]);
			if( strcmp(argv[1],"h")) {
				usage();
				exit(1);
			}
		}
	}
	run();
	return 0;
}

void run() {
	int serverfd;
	int tabletfd;
	fd_set readfds;
	char buf[BUFLEN];
	int maxfd;
	int nbytes;
	int sock_flags;
	int status;
	struct sockaddr_rc address = { 0 };
	struct sockaddr_rc remote  = { 0 };
	socklen_t opt = sizeof(remote);
	address.rc_family = AF_BLUETOOTH;
	address.rc_bdaddr = *BDADDR_ANY;
    address.rc_channel = (uint8_t) 1;

	printf("Opening socket to tablet ...\n");
	serverfd = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
	// Bind socket to port 1 of first available bluetooth adapter.
    bind(serverfd, (struct sockaddr *)&address, sizeof(address));
    // put socket into listening mode
    printf("%s: Socket bound and listening ...\n",PROG);
    listen(serverfd, 1); // Accepts one connection

    tabletfd = accept(serverfd, (struct sockaddr *)&remote, &opt);
    ba2str( &remote.rc_bdaddr, buf );
    printf("%s: Accepted connection from %s\n",PROG,buf);
	sock_flags = fcntl( tabletfd,F_GETFL,0);
	fcntl(tabletfd,F_SETFL,sock_flags|O_NONBLOCK);
	printf("Connected to tablet\n");

	for(;;) {
		// Loop waiting for activity on the ports
		maxfd = tabletfd;
		if( STDIN>tabletfd ) maxfd = STDIN;
		FD_ZERO(&readfds);
		FD_SET(STDIN,&readfds);
		FD_SET(tabletfd,&readfds);

		printf("%s",PROMPT);
		status = select(maxfd+1,&readfds,NULL,NULL,NULL);
		if( status>0 ) {
			if(FD_ISSET(tabletfd,&readfds) ) {
				memset(buf,'\0',BUFLEN);
				nbytes = recv(tabletfd, buf, BUFLEN, 0);
				if( nbytes==0 ) {
					printf("Connection to tablet closed]n");
					close(tabletfd);
					exit(0);					
				}
				else if(nbytes<0) {
					printf("Error on socket to tablet (%s)",strerror(errno));
					close(tabletfd);
					exit(1);
				}
				else {
					puts(buf);
					puts("\n");
					printf("%s",PROMPT);
				}
			}
			if(FD_ISSET(STDIN,&readfds) ) {
				fgets(buf,BUFLEN,stdin);	
				if( !strcmp(buf,"exit") || !strcmp(buf,"quit") ) break;
				nbytes = strlen(buf);
				send(tabletfd,buf,nbytes,0);
			}
		}
		else {
			printf("%s: Select returned an error ... aborting (%s)",PROG,strerror(errno));
			break;
		}
	}
	close(tabletfd);
	close(serverfd);
};

void usage() {
	printf("Usage:\n");
	printf("\t-h\t-help, print this text\n");
}
