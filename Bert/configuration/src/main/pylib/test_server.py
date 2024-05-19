#!/bin/python3
"""
   Copyright 2024. Charles Coughlin. All Rights Reserved.
   MIT License

   Command-line tool to mock the robot, a socket server
   There are no arguments. The host is always localhost.
   Make sure the actual robot is not running when this is used.
"""
import socket
import sys

class server():
    def __init__(self, *args):
        self.host = "127.0.0.1"
        self.port = 11046  # socket server port number

    def execute(self):
        try:
            server_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)  # iPv4, TCP
            print('Created server on port '+str(self.port))
            server_socket.bind((self.host, self.port))
            server_socket.listen()
            try:
                client,addr = server_socket.accept()
                print("  accepted client connection")
                print('  receive a message of form: MSG:text, then send ANS:response')
                while True:
                    data = client.recv(1024)
                    if not data:
                        break
                    msg = data.decode()
                    print('Received: ' + msg)  # show in terminal
                    # An empty message will be skipped
                    message = input("robot: ")  # take input
                    if message.lower().strip() == 'q' or message.lower().strip() == 'quit':
                        break
                    elif len(message)>0:
                        client.send(message.encode())  # send message

            except Exception as ex:
                print( "Exception reading ("+str(ex)+")")
            finally:
                client.close()

        except Exception as ex:
            print( "Failed to accept connection ("+str(ex)+")")
        finally:
            server_socket.close()  # close the connection

def main():
    app = server()
    app.execute()
    print('Done!')
    sys.exit(0)

if __name__ == '__main__':
    main()