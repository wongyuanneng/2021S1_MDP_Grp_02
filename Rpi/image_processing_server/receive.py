import imagezmq
image_hub = imagezmq.ImageHub(open_port = 'tcp://192.168.2.9:5555')
while True:
    _, frame = image_hub.recv_image()
    print(frame)
