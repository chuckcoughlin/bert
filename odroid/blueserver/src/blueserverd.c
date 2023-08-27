/*
 * Copyright 2019 Chuck Coughlin All rights reseerved.
 *  MIT License
 *
 * "bluetoothserverd" is a daemon that listens for requests
 * over the socket and bluetooth connections. Responses are
 * written to the appropriate counterparts. Logfile is presumed
 * to pre-exist. For "Bert" using rfcomm and select().
 *
 * See "Bluetooth Essentials for Programmers" by Albert Huang/Larry Rudolph
 *
 * Note: The Serial Port protocol must be configured prior to running:
 *             sdptool add SP
 */

#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <strings.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>
#include "blueserver.h"
char logbuf[512];
int logfd      = -1;
int robotfd    = -1;
int rserverfd  = -1;  // Server for robot
int tabletfd   = -1;
int tserverfd  = -1;  // Server for tablet


// There are no command-line arguments.
int main(int argc, char** argv) {
	bool daemon = true;
	char* logpath = LOGPATH;
	// The only command-line flag is -f for foreground.
	while(--argc) {
		char* arg = *(++argv);  // arg0 is the program name
		if(*arg=='-') {
			arg++;
			if( !strcmp(arg,"f")) {
				daemon = false;
			}
			else if(!strcmp(arg,"h")) {
				usage();
				exit(1);
			}
		}
		// Path for logging
		else {
			logpath=arg;
		}
	}
	logfd = open(logpath,O_APPEND|O_CREAT|O_SYNC|O_RDWR,0666);
	if(logfd<0) {
		printf("Failed to open logfile: %s (%s)\n",logpath,strerror(errno));
		exit(2);
	}

	if( daemon==true ) {
		pid_t pid = 0; // process ID
		pid = fork();
		if(pid < 0) {
			printf("%s: fork failed!\n",PROG);
			exit(1);
		}
		if( pid > 0 ) {
			close(logfd);
			exit(0);  // Parent process, just exit
		}
		chdir("/");   // Set working directory
		close(STDIN_FILENO);
		close(STDOUT_FILENO);
		close(STDERR_FILENO);
	}
	start();
	run();
	stop();
	return 0;
}

void run() {
	fd_set readfds;
	char buf[BUFLEN];
	int maxfd;
	int nbytes;
	int sock_flags;
	int status;
	struct sockaddr_rc address = { 0 };
	struct sockaddr_rc remote  = { 0 };
	address.rc_family = AF_BLUETOOTH;
	address.rc_bdaddr = *BDADDR_ANY;
    address.rc_channel = (uint8_t) 1;

	struct sockaddr_in robot;
	robot.sin_addr.s_addr = htonl(INADDR_LOOPBACK); /* localhost, 127.0.0.1 */
	robot.sin_family = AF_INET;
	robot.sin_port = htons( PORT );

	strcpy(logbuf,"Opening server socket for robot ...\n");
	write(logfd,logbuf,strlen(logbuf));
	rserverfd = socket(AF_INET, SOCK_STREAM, 0);
    bind(rserverfd, (struct sockaddr *)&robot, sizeof(robot));
	strcpy(logbuf,"Server socket bound and listening ...\n");
	write(logfd,logbuf,strlen(logbuf));

	strcpy(logbuf,"Opening server socket for tablet ...\n");
	write(logfd,logbuf,strlen(logbuf));
	tserverfd = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
	// Bind socket to port 1 of first available bluetooth adapter.
    bind(tserverfd, (struct sockaddr *)&address, sizeof(address));
	strcpy(logbuf,"Bluetooth server socket bound and listening ...\n");
	write(logfd,logbuf,strlen(logbuf));

	for(;;) {
		if( tabletfd<0 ) {
			strcpy(logbuf,"Listening for connection to the tablet\n");
			write(logfd,logbuf,strlen(logbuf));
    		listen(tserverfd, 1); // Accept one connection
			// Connect to the tablet
			socklen_t len = sizeof(remote);
    		tabletfd = accept(tserverfd, (struct sockaddr *)&remote,&len);
    		ba2str( &remote.rc_bdaddr, buf );
			snprintf(logbuf,sizeof(logbuf),"Accepted bluetooth connection from %s\n",buf);
			write(logfd,logbuf,strlen(logbuf));
			sock_flags = fcntl( tabletfd,F_GETFL,0);
			fcntl(tabletfd,F_SETFL,sock_flags|O_NONBLOCK);
		}

		if( robotfd<0 ) {
			strcpy(logbuf,"Listening for connection to the robot\n");
			write(logfd,logbuf,strlen(logbuf));
    		listen(rserverfd, 1); // Accept one connection
			// Connect to the robot
			socklen_t len = sizeof(robot);
			robotfd = accept(rserverfd,(struct sockaddr *)&robot, &len);
			if( robotfd<0 ) {
				snprintf(logbuf,sizeof(logbuf),"Error accepting connection to robot (%s)\n",strerror(errno));
				write(logfd,logbuf,strlen(logbuf));
				exit(2);
			}
			strcpy(logbuf,"Accepted robot connection\n");
			write(logfd,logbuf,strlen(logbuf));
		}

		// Both sides connected, loop waiting for activity on the ports
		maxfd = tabletfd;
		if( robotfd>tabletfd ) maxfd = robotfd;
		FD_ZERO(&readfds);
		FD_SET(robotfd,&readfds);
		FD_SET(tabletfd,&readfds);

		status = select(maxfd+1,&readfds,NULL,NULL,NULL);
		if( status>0 ) {
			// If set, there is data to be read
			if(FD_ISSET(robotfd,&readfds) ) {
				memset(buf,'\0',BUFLEN);
				nbytes = recv(robotfd, buf, BUFLEN, 0);
				if( nbytes==0 ) {
					strcpy(logbuf,"Connection to robot closed.\n");
					write(logfd,logbuf,strlen(logbuf));
					close(robotfd);
					robotfd = -1;
				}
				else if(nbytes<0) {
					snprintf(logbuf,sizeof(logbuf),"Error on socket to robot (%s)\n",strerror(errno));
					write(logfd,logbuf,strlen(logbuf));
					close(robotfd);
					robotfd = -1;
				}
				else {
					// Forward message to tablet. Text should have new-line.
					buf[nbytes] = '\0';  // Guarantee null-terminated
					snprintf(logbuf,sizeof(logbuf),"Sending to tablet: %s",buf);
					write(logfd,logbuf,strlen(logbuf));
					send(tabletfd, buf, nbytes, 0);
				}
			}
			if(FD_ISSET(tabletfd,&readfds) ) {
				memset(buf,'\0',BUFLEN);
				nbytes = recv(tabletfd, buf, BUFLEN, 0);
				if( nbytes==0 ) {
					strcpy(logbuf,"Connection to tablet closed.\n");
					write(logfd,logbuf,strlen(logbuf));
					close(tabletfd);
					tabletfd = -1;
				}
				else if(nbytes<0) {
					snprintf(logbuf,sizeof(logbuf),"Error on socket to tablet (%s)\n",strerror(errno));
					write(logfd,logbuf,strlen(logbuf));
					close(tabletfd);
					tabletfd = -1;
				}
				else {
					// Forward message to robot. Text should have a new-line.
					buf[nbytes] = '\0';  // Guarantee null-terminatedY
					snprintf(logbuf,sizeof(logbuf),"Sending to robot: %s",buf);
					write(logfd,logbuf,strlen(logbuf));
					send(robotfd, buf, nbytes, 0);
				}
			}
		}
		else {
			snprintf(logbuf,sizeof(logbuf),"Select returned an error ... aborting (%s)\n",strerror(errno));
			write(logfd,logbuf,strlen(logbuf));
			break;
		}
	}    // End of forever loop
}

void start() {
	snprintf(logbuf,sizeof(logbuf),"%s started ... \n",PROG);
	write(logfd,logbuf,strlen(logbuf));
}

void stop() {
	if(tabletfd>0) close(tabletfd);
	if(robotfd>0 ) close(robotfd);
	if(rserverfd>0 )close(rserverfd);
	if(tserverfd>0 )close(tserverfd);
	snprintf(logbuf,sizeof(logbuf),"%s shut down.\n",PROG);
	write(logfd,logbuf,strlen(logbuf));
	close(logfd);
}


void usage() {
	printf("Usage:\n");
	printf("\t-f\t-foreground, do not execute as a daemon\n");
	printf("\t-h\t-help, print this text\n");
	printf("\t<path>\t-path for logfile\n");
}
