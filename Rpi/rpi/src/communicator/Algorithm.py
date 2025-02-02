import socket
#import logging

from src.config import LOCALE, ALGORITHM_SOCKET_BUFFER_SIZE, WIFI_IP, WIFI_PORT, LOGSDIR


'''
Algorithm will need an accompanying (reference available in playgrounds pc_client.py
Algorithm.connect() will wait for Algorithm to connect before proceeding
'''

class Algorithm:
    def __init__(self, host=WIFI_IP, port=WIFI_PORT):
        #logging.basicConfig(filename = LOGSDIR+'alg.log', format = '%(asctime)s: %(message)s', filemode='w')
        #self.logger=logging.getLogger()
        ##self.logger.setLevel(logging.ERROR)
        #self.logger.setLevel(logging.DEBUG) #show all logs
        
        self.host = host
        self.port = port

        self.client_sock = None
        self.socket = None
        self.address = None

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.bind((self.host, self.port))
        self.socket.listen(1)
        
    def connect(self):
        while True:
            retry = False

            try:
                print('Establishing connection with Algorithm')

                if self.client_sock is None:
                    self.client_sock, self.address = self.socket.accept()
                    print('Successfully connected with Algorithm: ' + str(self.address))
                    retry = False

            except Exception as error:
                print('Connection with Algorithm failed: ' + str(error))

                if self.client_sock is not None:
                    self.client_sock.close()
                    self.client_sock = None
                retry = True

            if not retry:
                break
            print("Retrying Algorithm connection...")

    def disconnect(self):
        try:
            if self.client_sock is not None:
                self.client_sock.close()
                self.client_sock = None
            
            print("Algorithm disconnected Successfully")

        except Exception as error:
            print("Algorithm disconnect failed: " + str(error))

    def disconnect_all(self):
        try:
            if self.client_sock is not None:
                self.client_sock.close()
                self.client_sock = None

            if self.socket is not None:
                self.socket.close()
                self.socket = None

            print("Algorithm disconnected Successfully")

        except Exception as error:
            print("Algorithm disconnect failed: " + str(error))

    def read(self):
        try:
            message = self.client_sock.recv(ALGORITHM_SOCKET_BUFFER_SIZE).strip()

            if len(message) > 0:
                print('From Algorithm:')
                print(message)
                return message

            return None

        except Exception as error:
            print('Algorithm read failed: '+ str(error))
            raise error

    def write(self, message):
        try:
            print('To Algorithm:')
            print(message)
            self.client_sock.send(message)

        except Exception as error:
            print('Algorithm write failed: '+ str(error))
            raise error
            
