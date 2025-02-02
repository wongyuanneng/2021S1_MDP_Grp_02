import os
import argparse

from src.communicator.MultiProcessComms import MultiProcessComms
from src.config import IMAGE_PROCESSING_SERVER_URLS


parser = argparse.ArgumentParser(description='Main Program for MDP')
parser.add_argument(
    '-i', 
    '--image_recognition', 
    choices=IMAGE_PROCESSING_SERVER_URLS.keys(),
    default=None,
)

def init():
    args = parser.parse_args()
    image_processing_server = args.image_recognition

    os.system("sudo hciconfig hci0 piscan")

    try:
        multiprocess_communications = MultiProcessComms(
            IMAGE_PROCESSING_SERVER_URLS.get(image_processing_server)
        )
        multiprocess_communications.start()
    except Exception:
        multiprocess_communications.end()

if __name__ == '__main__':
    init()
