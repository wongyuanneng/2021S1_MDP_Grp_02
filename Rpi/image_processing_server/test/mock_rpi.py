import cv2
import imagezmq
import imutils
import sys
import os

image_sender = imagezmq.ImageSender(connect_to='tcp://127.0.0.1:5555')


print('Enter "q" to stop client, "e" to stop server and anything else to send an image:')
command = input()
i=0
while command != 'q' and i<291:
    x = "frame"+str(i)+".png"
    file_path = os.getcwd()+"\\..\\frames\\raw\\"+x
    print("file path: "+file_path)
    try:
        image = cv2.imread(file_path)
        if i==5:
            command = 'e'
        if command == 'e':
            image = cv2.imread('../stop_image_processing.png')
        
        reply = image_sender.send_image('image from client (RPi)', image)

        if reply is not None:
            reply = reply.decode('utf-8')
            
        if reply == 'End':
            print('Stopping image processing server.')
            break  # stop sending images
        else:
            print('From server:', reply)
    except Exception as error:
        i+=1
        continue
    #print('Enter "q" to stop client, "e" to stop server and anything else to send an image:')
    #command = input()
    i+=1
print('Stopping client.')
