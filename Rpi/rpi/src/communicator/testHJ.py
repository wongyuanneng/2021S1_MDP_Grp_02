import time
from datetime import datetime

from picamera import PiCamera
from picamera.array import PiRGBArray
import imagezmq
import cv2

def _take_pic():
    try:
        start_time = datetime.now()

        # initialize the camera and grab a reference to the raw camera capture
        camera = PiCamera(resolution=(1920, 1080))  # '1920x1080'
        rawCapture = PiRGBArray(camera)
        
        # allow the camera to warmup
        time.sleep(0.1)
        
        # grab an image from the camera
        camera.capture(rawCapture, format='bgr')
        image = rawCapture.array
        camera.close()

        print('Time taken to take picture: ' + str(datetime.now() - start_time) + 'seconds')
        
        # to gather training images
        # os.system("raspistill -o images/test"+
        # str(start_time.strftime("%d%m%H%M%S"))+".png -w 1920 -h 1080 -q 100")
    
    except Exception as error:
        print('Taking picture failed: ' + str(error))

    print(image.shape)
    return image

image_processing_server_url = 'tcp://192.168.2.6:5555'
image = _take_pic()
image_sender = imagezmq.ImageSender(connect_to=image_processing_server_url)
print('Enter "q" to stop client, "e" to stop server and anything else to send an image:')
command = input()

while command != 'q':
    if command == 'e':
        image = cv2.imread('stop_image_processing.png')
        reply = image_sender.send_image('image from client (RPi)', image)
    else:
        reply = image_sender.send_image('image from client (RPi)', _take_pic())

    if reply is not None:
        reply = reply.decode('utf-8')
        
    if reply == 'End':
        print('Stopping image processing server.')
        break  # stop sending images
    else:
        print('From server:', reply)

    print('Enter "q" to stop client, "e" to stop server and anything else to send an image:')
    command = input()

print('Stopping client.')
