/*
 * Copyright 2019 Chuck Coughlin All rights reseerved.
 *  MIT License
 *
 * "bluetoothserverd" is a daemon that listens for requests
 * over the socket and bluetooth connections. Responses are
 * written to the appropriate counterparts. Logging to syslog.
 * For "Bert" using rfcomm and select().
 *
 * See "Bluetooth Essentials for Programmers" by Albert Huang/Larry Rudolph
 *
 * Note: The Serial Port protocol must be configured prior to running.
 *             sdptool add SP
 */

#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <strings.h>
#include <stdlib.h>
#include <syslog.h>
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

int robotfd    = -1;
int rserverfd  = -1;  // Server for robot
int tabletfd   = -1;
int tserverfd  = -1;  // Server for tablet


// There are no command-line arguments.
int main(int argc, char* argv[]) {
	pid_t pid = 0; // process ID
	pid = fork();
	if(pid < 0) {
		printf("%s: fork failed!\n",PROG);
		exit(1);
	}
	if( pid > 0 ) {
		exit(0);  // Parent process, just exit
	}
	umask(0);
	chdir("/");   // Set working directory
	close(STDIN_FILENO);
	close(STDOUT_FILENO);
	close(STDERR_FILENO);
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

	printf("Opening server socket for tablet ...\n");
	serverfd = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
	// Bind socket to port 1 of first available bluetooth adapter.
    bind(serverfd, (struct sockaddr *)&address, sizeof(address));
    // put socket into listening mode
    printf("%s: Socket bound and listening ...\n",PROG);

	for(;;) {
		if( tabletfd<0 ) {
    		listen(serverfd, 1); // Accepts one connection
			// Connect to the tablet
    		tabletfd = accept(serverfd, (struct sockaddr *)&remote, &opt);
    		ba2str( &remote.rc_bdaddr, buf );
    		printf("%s: Accepted connection from %s\n",PROG,buf);
			sock_flags = fcntl( tabletfd,F_GETFL,0);
			fcntl(tabletfd,F_SETFL,sock_flags|O_NONBLOCK);
		}

		if( robotfd<0 ) {
			// Connect to the robot
			syslog(LOG_INFO,"%s: opening socket to the robot",PROG);
			robotfd = socket(AF_INET , SOCK_STREAM , 0);
			status = connect( robotfd, (struct sockaddr *)&robot , sizeof(robot));
			if( status!=0 && errno!=EAGAIN ) {
				syslog(LOG_WARNING,"%s: Error connecting to robot (%s)",PROG,strerror(errno));
				exit(2);
			}
			syslog(LOG_INFO,"%s: connected to robot",PROG);
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
					syslog(LOG_WARNING,"%s: Connection to robot closed",PROG);
					close(robotfd);
					robotfd = -1;
				}
				else if(nbytes<0) {
					syslog(LOG_WARNING,"%s: Error on socket to robot (%s)",PROG,strerror(errno));
					close(robotfd);
					robotfd = -1;
				}
				else {
					// Forward message to tablet
					syslog(LOG_INFO,"%s: Sending to tablet (%s)",PROG,buf);
					send(tabletfd, buf, nbytes, 0);
				}
			}
			if(FD_ISSET(tabletfd,&readfds) ) {
				memset(buf,'\0',BUFLEN);
				nbytes = recv(tabletfd, buf, BUFLEN, 0);
				if( nbytes==0 ) {
					syslog(LOG_WARNING,"%s: Connection to tablet closed",PROG);
					close(tabletfd);
					tabletfd = -1;
				}
				else if(nbytes<0) {
					syslog(LOG_WARNING,"%s: Error on socket to tablet (%s)",PROG,strerror(errno));
					close(tabletfd);
					tabletfd = -1;
				}
				else {
					// Forward message to robot
					syslog(LOG_INFO,"%s: Sending to robot (%s)",PROG,buf);
					send(robotfd, buf, nbytes, 0);
				}
			}
		}
		else {
			syslog(LOG_WARNING,"%s: Select returned an error ... aborting (%s)",PROG,strerror(errno));
			break;
		}
	}
}

void start() {
	setlogmask (LOG_UPTO (LOG_INFO));
	openlog("bert",LOG_CONS | LOG_PID | LOG_NDELAY, LOG_LOCAL1);
	syslog(LOG_INFO,"%s: started",PROG);
}

void stop() {
	closelog();
	if(tabletfd>0) close(tabletfd);
	if(robotfd>0 ) close(robotfd);
	if(rserverfd>0 )close(rserverfd);
	if(tserverfd>0 )close(tserverfd);
	syslog(LOG_INFO,"%s: shut down",PROG);
}
