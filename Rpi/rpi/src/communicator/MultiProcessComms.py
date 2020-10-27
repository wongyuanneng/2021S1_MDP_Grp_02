import time
from datetime import datetime
from multiprocessing import Process, Value
from multiprocessing.managers import BaseManager
import collections

import cv2
import imagezmq

from picamera import PiCamera
from picamera.array import PiRGBArray

from src.communicator.Android import Android
from src.communicator.Arduino import Arduino
from src.communicator.Algorithm import Algorithm
from src.config import STOPPING_IMAGE, IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, LOGSDIR, TARGET_IMG_COUNT
from src.protocols import *
#import logging

class DequeManager(BaseManager):
    pass

class DequeProxy(object):
    def __init__(self, *args):
        self.deque = collections.deque(*args)
    def __len__(self):
        return self.deque.__len__()
    def appendleft(self, x):
        self.deque.appendleft(x)
    def append(self, x):
        self.deque.append(x)
    def popleft(self):
        return self.deque.popleft()

DequeManager.register('DequeProxy', DequeProxy,
                      exposed=['__len__', 'append', 'appendleft', 'popleft'])


class MultiProcessComms:
    """
    This class handles multi-processing communications between Arduino, Algorithm and Android.
    """
    def __init__(self, image_processing_server_url: str=None):
        """
        Instantiates a MultiProcess Communications session and set up the necessary variables.

        Upon instantiation, RPi begins connecting to
        - Arduino
        - Algorithm
        - Android
        in this exact order.

        Also instantiates the queues required for multiprocessing.
        """
        #logging.basicConfig(filename = LOGSDIR+'mpc.log', format = '%(asctime)s: %(message)s', filemode='w')
        #self.logger=logging.getLogger()
        #Only levels higher than the set-level will be logged.
        #self.logger.setLevel(logging.ERROR)

        print('Initializing Multiprocessing Communication')

        self.arduino = Arduino()  # handles connection to Arduino
        self.algorithm = Algorithm()  # handles connection to Algorithm
        self.android = Android()  # handles connection to Android

        self.manager = DequeManager()
        self.manager.start()

        # messages from Arduino, Algorithm and Android are placed in this queue before being read
        self.message_deque = self.manager.DequeProxy()
        self.to_android_message_deque = self.manager.DequeProxy()

        self.read_arduino_process = Process(target=self._read_arduino)
        self.read_algorithm_process = Process(target=self._read_algorithm)
        self.read_android_process = Process(target=self._read_android)

        self.write_process = Process(target=self._write_target)
        self.write_android_process = Process(target=self._write_android)


        # the current action / status of the robot
        self.status = Status.IDLE  # robot starts off being idle

        self.dropped_connection = Value('i',0) # 0 - arduino, 1 - algorithm

        self.image_process = None

        if image_processing_server_url is not None:

            self.image_process = Process(target=self._process_pic)
            self.image_capture_process = Process(target=self._process_capture_pic)

            # pictures taken using the PiCamera are placed in this queue
            self.image_deque = self.manager.DequeProxy()

            # Communicate with camera process
            self.image_capture_deque = self.manager.DequeProxy()

            self.image_processing_server_url = image_processing_server_url
            self.image_count = Value('i',0)


    def start(self):
        try:
            self.arduino.connect()
            self.algorithm.connect()
            self.android.connect()

            print('Connected to Arduino, Algorithm and Android')

            self.read_arduino_process.start()
            self.read_algorithm_process.start()
            self.read_android_process.start()
            self.write_process.start()
            self.write_android_process.start()

            if self.image_process is not None:
                self.image_process.start()
                self.image_capture_process.start()

            print('Started all processes: read-arduino, read-algorithm, read-android, write, image')

            print('Multiprocess communication session started')

        except Exception as error:
            raise error

        self._allow_reconnection()


    def end(self):
        # children processes should be killed once this parent process is killed
        self.algorithm.disconnect_all()
        self.android.disconnect_all()
        print('Multiprocess communication session ended')

    def _allow_reconnection(self):
        #print('You can reconnect to RPi after disconnecting now')

        while True:
            try:
                if not self.read_arduino_process.is_alive():
                    self._reconnect_arduino()

                if not self.read_algorithm_process.is_alive():
                    self._reconnect_algorithm()

                if not self.read_android_process.is_alive():
                    self._reconnect_android()

                if not self.write_process.is_alive():
                    if self.dropped_connection.value == 0:
                        self._reconnect_arduino()
                    elif self.dropped_connection.value == 1:
                        self._reconnect_algorithm()

                if not self.write_android_process.is_alive():
                    self._reconnect_android()

                if self.image_process is not None and not self.image_process.is_alive():
                   self.image_process.terminate()
                   self.image_capture_process.terminate()

            except Exception as error:
                print("Error during reconnection: ",error)
                raise error

    def _reconnect_arduino(self):
        self.arduino.disconnect()

        self.read_arduino_process.terminate()
        self.write_process.terminate()
        self.write_android_process.terminate()

        self.arduino.connect()

        self.read_arduino_process = Process(target=self._read_arduino)
        self.read_arduino_process.start()

        self.write_process = Process(target=self._write_target)
        self.write_process.start()

        self.write_android_process = Process(target=self._write_android)
        self.write_android_process.start()

        print('Reconnected to Arduino')

    def _reconnect_algorithm(self):
        self.algorithm.disconnect()

        self.read_algorithm_process.terminate()
        self.write_process.terminate()
        self.write_android_process.terminate()

        self.algorithm.connect()

        self.read_algorithm_process = Process(target=self._read_algorithm)
        self.read_algorithm_process.start()

        self.write_process = Process(target=self._write_target)
        self.write_process.start()

        self.write_android_process = Process(target=self._write_android)
        self.write_android_process.start()

        print('Reconnected to Algorithm')

    def _reconnect_android(self):
        self.android.disconnect()

        self.read_android_process.terminate()
        self.write_process.terminate()
        self.write_android_process.terminate()

        self.android.connect()

        self.read_android_process = Process(target=self._read_android)
        self.read_android_process.start()

        self.write_process = Process(target=self._write_target)
        self.write_process.start()

        self.write_android_process = Process(target=self._write_android)
        self.write_android_process.start()

        print('Reconnected to Android')

    def _read_arduino(self):
        while True:
            try:
                raw_message = self.arduino.read()

                if raw_message is None:
                    continue
                message_list = raw_message.splitlines()

                for message in message_list:

                    if len(message) <= 0:
                        continue

                    self.message_deque.append(self._format_for(
                        ALGORITHM_HEADER,
                        message + NEWLINE
                    ))

            except Exception as error:
                print('Process read_arduino failed: ' + str(error))
                break

    def _read_algorithm(self):
        while True:
            try:
                raw_message = self.algorithm.read()

                if raw_message is None:
                    continue

                message_list = raw_message.splitlines()

                for message in message_list:

                    if len(message) <= 0:
                        continue

                    elif message[0] == AlgorithmToRPi.TAKE_PICTURE:
                        #on TARGET_IMG_COUNT, ask algo to stop capturing img
                        if self.image_count.value >= TARGET_IMG_COUNT:
                            self.message_deque.append(self._format_for(
                                ALGORITHM_HEADER,
                                RPiToAlgorithm.DONE_IMG_REC + NEWLINE
                            ))
                            self.image_deque.append([cv2.imread(STOPPING_IMAGE),"-1,-1|-1,-1|-1,-1"])

                        else:

                            message = message[2:-1]  # to remove 'C[' and ']'
                            self.to_android_message_deque.append(
                                RPiToAndroid.STATUS_TAKING_PICTURE + NEWLINE
                            )\

                            # Inform camera procecss to take photo
                            self.image_capture_deque.append([message])


                    elif message[-1:] == AlgorithmToRPi.EXPLORATION_COMPLETE:
                        self.to_android_message_deque.append(
                            RPiToAndroid.EXPLORE_COMPLETE + NEWLINE
                        )
                        # to let image processing server end all processing and display all images
                        self.status = Status.IDLE

                        try:
                            self.image_deque.append([cv2.imread(STOPPING_IMAGE),"-1,-1|-1,-1|-1,-1".encode()])

                        except Exception as error:
                            print('Failed to stop image server: ' + str(error))
                            #carry on

                        self.to_android_message_deque.append(
                            RPiToAndroid.STATUS_IDLE + NEWLINE
                        )


                    elif message[0] == AlgorithmToAndroid.MDF_STRING:
                        self.to_android_message_deque.append(
                            message[1:] + NEWLINE
                        )

                    else:  # (message[0]=='W' or message in ['D|', 'A|', 'Z|']):
                        self._forward_message_algorithm_to_android(message)
                        self.message_deque.append(self._format_for(
                            ARDUINO_HEADER,
                            message + NEWLINE
                        ))

            except Exception as error:
                print('Process read_algorithm failed: ' + str(error))
                break

    def _forward_message_algorithm_to_android(self, message):
        messages_for_android = message.split(MESSAGE_SEPARATOR)

        for message_for_android in messages_for_android:

            if len(message_for_android) <= 0:
                continue

            elif message_for_android[0] == AlgorithmToAndroid.TURN_LEFT:
                self.to_android_message_deque.append(
                    RPiToAndroid.TURN_LEFT + NEWLINE
                )

                # self.to_android_message_deque.append(
                    # RPiToAndroid.STATUS_TURNING_LEFT + NEWLINE
                # )

            elif message_for_android[0] == AlgorithmToAndroid.TURN_RIGHT:
                self.to_android_message_deque.append(
                    RPiToAndroid.TURN_RIGHT + NEWLINE
                )

                # self.to_android_message_deque.append(
                    # RPiToAndroid.STATUS_TURNING_RIGHT + NEWLINE
                # )

            # elif message_for_android[0] == AlgorithmToAndroid.CALIBRATING_CORNER:
                # self.to_android_message_deque.append(
                    # RPiToAndroid.STATUS_CALIBRATING_CORNER + NEWLINE
                # )

            # elif message_for_android[0] == AlgorithmToAndroid.SENSE_ALL:
                # self.to_android_message_deque.append(
                    # RPiToAndroid.STATUS_SENSE_ALL + NEWLINE
                # )

            # elif message_for_android[0] == AlgorithmToAndroid.ALIGN_RIGHT:
                # self.to_android_message_deque.append(
                    # RPiToAndroid.STATUS_ALIGN_RIGHT + NEWLINE
                # )

            # elif message_for_android[0] == AlgorithmToAndroid.ALIGN_FRONT:
                # self.to_android_message_deque.append(
                    # RPiToAndroid.STATUS_ALIGN_FRONT + NEWLINE
                # )

            elif message_for_android[0] == AlgorithmToAndroid.MOVE_FORWARD:
                # if self.status == Status.EXPLORING:
                    # self.to_android_message_deque.append(
                        # RPiToAndroid.STATUS_EXPLORING + NEWLINE
                    # )

                # elif self.status == Status.FASTEST_PATH:
                    # self.to_android_message_deque.append(
                        # RPiToAndroid.STATUS_FASTEST_PATH + NEWLINE
                    # )
                num_steps_forward = int(message_for_android.decode()[1:])

                # TODO
                #print('Number of steps to move forward: %s'%str(num_steps_forward))

                for _ in range(num_steps_forward):
                    self.to_android_message_deque.append(
                        RPiToAndroid.MOVE_UP + NEWLINE
                    )

                    self.to_android_message_deque.append(
                        RPiToAndroid.STATUS_MOVING_FORWARD + NEWLINE
                    )

    def _read_android(self):
        while True:
            try:
                raw_message = self.android.read()

                if raw_message is None:
                    continue

                message_list = raw_message.splitlines()

                for message in message_list:
                    if len(message) <= 0:
                        continue

                    elif message in (AndroidToArduino.ALL_MESSAGES + [AndroidToRPi.CALIBRATE_SENSOR]):
                        if message == AndroidToRPi.CALIBRATE_SENSOR:
                            self.message_deque.append(self._format_for(
                                ARDUINO_HEADER,
                                RPiToArduino.CALIBRATE_SENSOR + NEWLINE
                            ))

                        else:
                            self.message_deque.append(self._format_for(
                                ARDUINO_HEADER, message + NEWLINE
                            ))

                    else:  # if message in ['ES|', 'FS|', 'SendArena']:
                        if message == AndroidToAlgorithm.START_EXPLORATION:
                            self.status = Status.EXPLORING
                            self.message_deque.append(self._format_for(
                                ARDUINO_HEADER,
                                RPiToArduino.START_EXPLORATION + NEWLINE
                            ))

                        elif message == AndroidToAlgorithm.START_FASTEST_PATH:
                            self.status = Status.FASTEST_PATH
                            self.message_deque.append(self._format_for(
                                ARDUINO_HEADER,
                                RPiToArduino.START_FASTEST_PATH + NEWLINE
                            ))

                        self.message_deque.append(self._format_for(
                            ALGORITHM_HEADER,
                            message + NEWLINE
                        ))

            except Exception as error:
                print('Process read_android failed: ' + str(error))
                break

    def _write_target(self):
        while True:
            target = None
            try:
                if self.message_deque:
                    message = self.message_deque.popleft()
                    target, payload = message['target'], message['payload']

                    if target == ARDUINO_HEADER:
                        self.arduino.write(payload)

                    elif target == ALGORITHM_HEADER:
                        self.algorithm.write(payload)

                    else:
                        print("Invalid header", target)

            except Exception as error:
                print('Process write_target failed: ' + str(error))

                if target == ARDUINO_HEADER:
                    self.dropped_connection.value = 0

                elif target == ALGORITHM_HEADER:
                    self.dropped_connection.value = 1

                self.message_deque.appendleft(message)

                break

    def _write_android(self):
        while True:
            try:
                if self.to_android_message_deque:
                    message = self.to_android_message_deque.popleft()

                    self.android.write(message)

            except Exception as error:
                print('Process write_android failed: ' + str(error))
                self.to_android_message_deque.appendleft(message)
                break

    def _process_pic(self):
        # initialize the ImageSender object with the socket address of the server
        image_sender = imagezmq.ImageSender(
            connect_to=self.image_processing_server_url)
        image_id_list = []
        while True:
            try:
                if self.image_deque:
                    start_time = datetime.now()

                    image_message =  self.image_deque.popleft()
                    # format: 'x,y|x,y|x,y'
                    obstacle_coordinates = image_message[1]

                    obstacle_coordinates = obstacle_coordinates.decode('utf-8')

                    reply = image_sender.send_image(
                        'image from RPi',
                        image_message[0]
                    )

                    print('From server:', reply)

                    if reply is not None:
                        reply = reply.decode('utf-8')
                    else:
                        pass

                    if reply == 'End':
                        print('Stopping image processing server.')
                        self.image_capture_deque.append('End')
                        break  # stop sending images

                    # example replies
                    # "1|2|3" 3 symbols in order from left to right
                    # "1|-1|3" 2 symbols, 1 on the left, 1 on the right
                    # "1" 1 symbol either on the left, middle or right
                    else:
                        detections = reply.split(MESSAGE_SEPARATOR.decode('utf-8'))
                        obstacle_coordinate_list = obstacle_coordinates.split(MESSAGE_SEPARATOR.decode('utf-8'))
                        for detection, coordinates in zip(detections, obstacle_coordinate_list):
                            if coordinates == '-1,-1':
                                continue  # if no obstacle, skip mapping of symbol id
                            elif detection == '-1':
                                continue  # if no symbol detected, skip mapping of symbol id
                            else:
                                id_string_to_android = '{"image":[' + detection + \
                                ',' + coordinates + ']}'
                                #print(id_string_to_android)

                                if detection not in image_id_list:
                                    self.image_count.value += 1
                                    image_id_list.append(detection)

                                self.to_android_message_deque.append(
                                    id_string_to_android.encode() + NEWLINE
                                )

                    #print('Time taken to process image: ' + \
                     #   str(datetime.now() - start_time) + ' seconds')

            except Exception as error:
                print('Image processing failed: ' + str(error))

        print("Process _process_pic() killed.")

    def _format_for(self, target, payload):
        return {
            'target': target,
            'payload': payload,
        }

    def _process_capture_pic(self):
        # initialize the camera and grab a reference to the raw camera capture
        camera = PiCamera(resolution=(IMAGE_WIDTH, IMAGE_HEIGHT))  # '1920x1080'
        camera.rotation = 180
        camera.hflip = True
        while True:
            try:
                if self.image_capture_deque:
                    image_message =  self.image_capture_deque.popleft()
                    if (image_message == 'End'):
                        break
                    message = image_message[0]

                    try:
                        #start_time = datetime.now()
                        rawCapture = PiRGBArray(camera)
                        #
                        # # allow the camera to warmup
                        # time.sleep(0.1)

                        # grab an image from the camera
                        camera.capture(rawCapture, format=IMAGE_FORMAT)
                        image = rawCapture.array


                        #print('Time taken to take picture: ' + str(datetime.now() - start_time) + 'seconds')

                        # to gather training images
                        #os.system("raspistill -o images/test"+
                        #           str(start_time.strftime("%d%m%H%M%S"))+".png -w 1920 -h 1080 -q 100")

                    except Exception as error:
                        print('Taking picture failed: ' + str(error))

                    self.message_deque.append(self._format_for(
                        ALGORITHM_HEADER,
                        RPiToAlgorithm.DONE_TAKING_PICTURE + NEWLINE
                    ))
                    self.image_deque.append([image,message])

            except Exception as error:
                print('Image capture failed: ' + str(error))

        camera.close()
        print("Process _process_capture_pic() killed.")
