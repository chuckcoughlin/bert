#!/bin/python3
"""
   Copyright 2024. Charles Coughlin. All Rights Reserved.
   MIT License

   Command-line tool to mock the tablet's socket communications.
   The command line argument is the server IP address (10.0.0.42).
   If none is given, assume localhost.

   Legal messages are in the form: HDR:text where HDR is a legal message type
     LOG - a notification, no response
     MSG - either a request or command. There will always be a response.
"""
import socket
import sys

class client():
    # There is always a single argument
    def __init__(self, *args):
        self.host = args[0]    # localhost or as supplied on command-line
        self.port = 11046      # socket server port number

    def execute(self):
        client_socket = socket.socket()  # instantiate
        print('  connecting to '+self.host+' ('+str(self.port)+')')
        print('  send message of form: MSG:text, then wait for response')
        try:
            client_socket.connect((self.host, self.port))  # connect to the server

            message = input("tablet: ")  # take input

            while message.lower().strip() != 'q' and message.lower().strip() != 'quit':
                client_socket.send(message.encode())  # send message
                if len(message)>3 and message[0:3]=='MSG':
                    data = client_socket.recv(1024).decode()  # receive response
                    print('Received: ' + data)    # show in terminal
                message = input(" tablet: ")  # again read input

            client_socket.close()  # close the connection
        except Exception as ex:
            print( "Failed to connnect ("+str(ex)+")")

def main():
    if len(sys.argv)<2 :
        host = "127.0.0.1"
    else:
        host = sys.argv[1]

    app = client(host)
    app.execute()
    print('Done!')
    sys.exit(0)

if __name__ == '__main__':
    main()
