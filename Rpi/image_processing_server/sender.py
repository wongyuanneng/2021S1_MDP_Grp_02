import imagezmq
import cv2
image = cv2.imread('interstellar_blackhole.jpg')
sender = imagezmq.ImageSender(connect_to= 'tcp://192.168.2.9:5555')
sender.send_image(
    'image from RPi',
    image
)
print("Done!")
