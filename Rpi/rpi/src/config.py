#import logging

LOCALE = 'UTF-8'
LOGSDIR = "src/communicator/logs/"

# Android BT connection settings
RFCOMM_CHANNEL = 7
RPI_MAC_ADDR = 'B8:27:EB:C0:5E:80'
UUID = '443559ba-b80f-4fb6-99d9-ddbcd6138fbd'
ANDROID_SOCKET_BUFFER_SIZE = 512

# Algorithm Wifi connection settings
# raspberryHotPotato: 192.168.3.1
WIFI_IP = '192.168.2.2'
WIFI_PORT = 8080
ALGORITHM_SOCKET_BUFFER_SIZE = 512

# Arduino USB connection settings
#SERIAL_PORT = '/dev/ttyACM0'
# Symbolic link to always point to the correct port that arduino is connected to
SERIAL_PORT = '/dev/serial/by-id/usb-Arduino__www.arduino.cc__0043_75232303235351A040B1-if00'
BAUD_RATE = 115200

# Image Recognition Settings
STOPPING_IMAGE = 'stop_image_processing.png'
TARGET_IMG_COUNT = 4

IMAGE_WIDTH = 1920
IMAGE_HEIGHT = 1080
IMAGE_FORMAT = 'bgr'

BASE_IP = 'tcp://192.168.2.'
PORT = ':5555'

IMAGE_PROCESSING_SERVER_URLS = {
    'yuanneng': BASE_IP + '9' + PORT,
    'hongjun': BASE_IP + '7' + PORT,
    'alex': BASE_IP + '10' + PORT,
    'kevin': BASE_IP + '8' + PORT
}


#LOGGING_PARAMETERS = {
#    'D':logging.DEBUG,    #level 1: not used
#    'I':logging.INFO,     #level 2: set to log all other print statements + levels 3,4,5
#    'W':logging.WARNING,  #level 3: not used.
#    "E":logging.ERROR,    #level 4: set to log all exceptions only + level 5
#    "C":logging.CRITICAL  #level 5: set to log literally NOTHING.
#}
