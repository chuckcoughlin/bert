/*
 * Copyright 2019-2024 Chuck Coughlin All rights reserved.
 *  MIT License
 *
 * Functions implemented in the daemon.
 */
void run();
void start();
void stop();
void usage();

int BUFLEN = 1024;
char* DEST = "4C:2E:5E:26:C6:0C";  // Tablet bluetooth address
char* HOST = "localhost";
int   PORT = 11046;               // Must match value in bert.xml
char* PROMPT = "bert: ";
char* PROG = "blueserverd";
char* LOGPATH = "/usr/local/robot/logs/blueserverd.log";
