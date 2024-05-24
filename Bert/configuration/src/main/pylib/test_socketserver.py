#!/bin/python3
"""
   Copyright 2024. Charles Coughlin. All Rights Reserved.
   MIT License

   Command-line tool to mock the robot, a socket server
   There are no arguments. The host is always localhost.
   Make sure the actual robot is not running when this is used.
"""
import socketserver
import sys

class TCPHandler(socketserver.BaseRequestHandler):
    def handle(self):
        print("  accepted client connection")
        print("  send an initial greeting, then")
        print('  receive a message of form: MSG:text, then send ANS:response')
        message = "Connected to mock robot\n"
        self.request.sendall(message.encode())
        while True:
            data = self.request.recv(1024)
            if not data:
                break
            msg = data.decode()
            print('Received: ' + msg)  # show in terminal
            # An empty message will be skipped
            message = input("robot: ")  # take input
            if message.lower().strip() == 'q' or message.lower().strip() == 'quit':
                break
            elif len(message)>0:
                message +='\n'
                self.request.sendall(message.encode())  # send message

def main():
    HOST, PORT = "localhost",11046
    with socketserver.TCPServer((HOST,PORT),TCPHandler) as server:
        server.serve_forever()
    print('Done!')
    sys.exit(0)

if __name__ == '__main__':
    main()