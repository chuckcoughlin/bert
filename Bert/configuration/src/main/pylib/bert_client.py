import socket


def client_program():
    host = socket.gethostname()  # as both code is running on same pc
    port = 11046  # socket server port number

    client_socket = socket.socket()  # instantiate
    client_socket.connect((host, port))  # connect to the server

    message = input("bert: ")  # take input

    while message.lower().strip() != 'q' and message.lower().strip() != 'quit':
        client_socket.send(message.encode())  # send message
        data = client_socket.recv(1024).decode()  # receive response

        print('Received from server: ' + data)  # show in terminal

        message = input(" -> ")  # again take input

    client_socket.close()  # close the connection
