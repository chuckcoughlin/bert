/*
 * Copyright 2019 Chuck Coughlin All rights reseerved.
 *  MIT License
 *
 * Functions implemented in the daemon.
 */
void run();
void start();
void stop();
void usage();

int BUFLEN = 1024;
char* DEST = "C0:D3:C0:72:94:6A";
char* HOST = "localhost";
int   PORT = 11046;  // Must match value in bert.xml
char* PROMPT = "bert: ";
char* PROG = "blueserverd";
char* LOGPATH = "var/log/blueserverd.log";