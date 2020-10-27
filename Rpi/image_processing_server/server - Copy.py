from object_detection import ObjectDetection
from image_receiver import custom_imagezmq as imagezmq
from datetime import datetime
import cv2

import os
import shutil
import sys
import random

import imutils
import numpy as np
import tensorflow as tf
from PIL import Image, ImageDraw, ImageFont

from config import *
from utils import label_map_util
from utils import visualization_utils as vis_util

sys.path.append("..")

# Grab path to current working directory
cwd_path = os.getcwd()

class TFObjectDetection(ObjectDetection):
    """Object Detection class for TensorFlow"""

    def __init__(self, graph_def, labels):
        super(TFObjectDetection, self).__init__(labels)
        self.graph = tf.compat.v1.Graph()
        with self.graph.as_default():
            input_data = tf.compat.v1.placeholder(
                tf.float32, [1, None, None, 3], name='Placeholder')
            tf.import_graph_def(graph_def, input_map={
                                "Placeholder:0": input_data}, name="")

    def predict(self, preprocessed_image):
        inputs = np.array(preprocessed_image, dtype=np.float)[
            :, :, (2, 1, 0)]  # RGB -> BGR

        with tf.compat.v1.Session(graph=self.graph) as sess:
            output_tensor = sess.graph.get_tensor_by_name('model_outputs:0')
            outputs = sess.run(
                output_tensor, {'Placeholder:0': inputs[np.newaxis, ...]})
            return outputs[0]

class ImageProcessingServer:
    def __init__(self):
        #Model
        # Load a TensorFlow model
        graph_def = tf.compat.v1.GraphDef()
        with tf.io.gfile.GFile(MODEL_FILENAME, 'rb') as f:
            graph_def.ParseFromString(f.read())

        # print([n.name for n in graph_def.node])

        # Load labels
        with open(LABELS_FILENAME, 'r') as f:
            labels = [l.strip() for l in f.readlines()]

        self.od_model = TFObjectDetection(graph_def, labels)

        # Load the label map.
        #
        # Label maps map indices to category names, so that when our convolution
        # network predicts `0`, we know that this corresponds to `white up arrow`.
        #
        # Here we use internal utility functions, but anything that returns a
        # dictionary mapping integers to appropriate string labels would be fine

        label_map = label_map_util.load_labelmap(CATEGORY_FILENAME)
        categories = label_map_util.convert_label_map_to_categories(
            label_map, 
            max_num_classes=NUM_CLASSES, 
            use_display_name=True
        )

        self.category_index = label_map_util.create_category_index(categories)


        #Directories
        self._initialise_directories()
        #Assume all directories present

        # image to be sent from RPi to stop this server
        self.stopping_image = imutils.resize(cv2.imread(STOPPING_IMAGE), width=IMAGE_WIDTH)
        
        # initialize the ImageHub object
        self.image_hub = imagezmq.CustomImageHub()

        self.frame_list = []  # list of frames with detections
        
    def _initialise_directories(self):
        image_dir_path = os.path.join(cwd_path, MAIN_IMAGE_DIR)

        #if os.path.exists(image_dir_path):
        #    shutil.rmtree(image_dir_path)
        #    return

        self.raw_image_dir_path = os.path.join(image_dir_path, RAW_IMAGE_DIR)
        #os.makedirs(self.raw_image_dir_path)

        self.processed_image_dir_path = os.path.join(image_dir_path, PROCESSED_IMAGE_DIR)
        #os.makedirs(self.processed_image_dir_path)

    def _is_stopping_frame(self, frame):
        difference = cv2.subtract(frame, self.stopping_image)
        return not np.any(difference)

    def _show_all_images(self):
        """
        return:
            whether key pressed is 'r'
        """
        print('Showing all detected images')
        frames = []
        # show all images with detections
        for index, frame_path in enumerate(self.frame_list):
            frame = cv2.imread(frame_path)
            frame = imutils.resize(frame, width=DISPLAY_IMAGE_WIDTH)
            
            frames.append(frame)
            if (len(frames)==5):
                break

        while (len(frames)<6):
            redundant_img = imutils.resize(self.stopping_image, width=400) #fill the last frame
            frames.append(redundant_img)
            
        
        numpy_vertical1 = np.vstack((frames[0], frames[1]))
        numpy_vertical2 = np.vstack((frames[2], frames[3]))
        numpy_vertical3 = np.vstack((frames[4], frames[5]))
        numpy_horizontal = np.hstack((numpy_vertical1, numpy_vertical2, numpy_vertical3))
        
        cv2.imshow('Images rec-ed', numpy_horizontal)
        cv2.imwrite('img_rec_output.jpg', numpy_horizontal)
        
        keycode = cv2.waitKey(0)

        cv2.destroyAllWindows()

        # https://stackoverflow.com/q/57690899/9171260
        return keycode & 0xFF == ord('r')
    
    def _get_obstacle_map(self, bounding_boxes, classes, scores):
        """
        params:
        - bbox_list (list): [
            [top_left_y (float), top_left_x (float), bot_right_y (float), bot_right_x (float)], 
            ..., 
        ]
        - class_list (list): [class_id (int), ]
        - score_list (list): [confidence_score (float)]

        return: (
            { LEFT_OBSTACLE: SYMBOL, MIDDLE_OBSTACLE: SYMBOL, RIGHT_OBSTACLE: SYMBOL }, 
            true positive bounding boxes (list), 
            true positive classes (list), 
            true positive confidence scores (list),
        )
        """

        #bounding_boxes, classes, scores = [], [], []
        
        # -1 means no detection for that obstacle
        obstacle_symbol_map = {
            LEFT_OBSTACLE: NO_SYMBOL,
            MIDDLE_OBSTACLE: NO_SYMBOL,
            RIGHT_OBSTACLE: NO_SYMBOL,
        }

        num_symbols = 0
        
        left_xmax = float('-inf')
        right_xmin = float('inf')
        FAR_SYMBOL = NO_SYMBOL
        
        for bbox, class_id, score in zip(bounding_boxes, classes, scores):
            if num_symbols >= MAX_NUM_SYMBOLS:
                break

            top_left_y, top_left_x, bot_right_y, bot_right_x = tuple(bbox)

            top_left_y = top_left_y
            top_left_x = top_left_x
            bot_right_y = bot_right_y
            bot_right_x = bot_right_x
			
            #not_red = class_id != 4 and class_id != 8 and class_id != 11

            # false positive if:
            # confidence score is lower than a generic threshold (for all classes)
            # confidence score is lower than a higher threshold (for non-reds)
            # the bottom y-coordinate is lower than its repective threshold (too far)

            if ((score >= MIN_CONFIDENCE_THRESHOLD) and (bot_right_y > YMAX_THRESHOLD or bot_right_y < YMAX_THRESHOLD)):
                FAR_OBSTACLE = str(class_id)
            
            if ((score <= MIN_CONFIDENCE_THRESHOLD)or (bot_right_y < YMAX_THRESHOLD)): 
                continue  # false positive -> skip
            

            # obstacle already has a symbol of higher confidence,
            # and is directly to the left of middle
            # symbol left
            if (bot_right_x < SYMBOL_ON_LEFT_OF_IMAGE_THRESHOLD):  
                if obstacle_symbol_map[LEFT_OBSTACLE] != NO_SYMBOL and bot_right_x < left_xmax:  
                    continue  
                
                left_xmax = bot_right_x
                obstacle_symbol_map[LEFT_OBSTACLE] = str(class_id)

            # obstacle already has a symbol of higher confidence,
            # and is directly to the right of middle
            # symbol right
            elif (top_left_x  > SYMBOL_ON_RIGHT_OF_IMAGE_THRESHOLD):  
                if obstacle_symbol_map[RIGHT_OBSTACLE] != NO_SYMBOL and top_left_x > right_xmin:
                    continue  
                
                right_xmin = top_left_x
                obstacle_symbol_map[RIGHT_OBSTACLE] = str(class_id)

            else:  # symbol middle
                # obstacle already has a symbol of higher confidence
                if obstacle_symbol_map[MIDDLE_OBSTACLE] != NO_SYMBOL:
                    continue  
                obstacle_symbol_map[MIDDLE_OBSTACLE] = str(class_id)

            if FAR_OBSTACLE != NO_SYMBOL:
                if(obstacle_symbol_map[LEFT_OBSTACLE] == NO_SYMBOL and obstacle_symbol_map[RIGHT_OBSTACLE] == NO_SYMBOL and obstacle_symbol_map[MIDDLE_OBSTACLE] == NO_SYMBOL):
                    obstacle_symbol_map[MIDDLE_OBSTACLE] = FAR_OBSTACLE
                    
                        

            #bounding_boxes.append(bbox)
            #classes.append(class_id)
            #scores.append(score)
        
            print(
                'id: ', class_id,
                'confidence: ', '{:.3f}'.format(score),
                '\n',
                'xmin: ', '{:.3f}'.format(top_left_x),
                'xmax: ', '{:.3f}'.format(bot_right_x),
                'ymax: ', '{:.3f}'.format(bot_right_y),
                '\n',
            )

            num_symbols += 1

        return obstacle_symbol_map

    def drawBB(self, prediction, image):
        shape = [prediction["boundingBox"]["left"]*image.size[0],
                 prediction["boundingBox"]["top"]*image.size[1],
                 (prediction["boundingBox"]["width"] +
                  prediction["boundingBox"]["left"])*image.size[0],
                 (prediction["boundingBox"]["top"] +
                  prediction["boundingBox"]["height"])*image.size[1]]
        imgTool = ImageDraw.Draw(image)
        imgTool.text((shape[0], shape[1]), "id:"+prediction["tagName"] + " |Acc:" +
                  str(prediction["probability"]), font=ImageFont.truetype("arial.ttf", 30))
        imgTool.rectangle(shape, outline="red", width=2)
        return image
    
    def start(self):
        print('\nStarted image processing server.\n')
        count=0
        while True:
            print('Waiting for image from RPi...')

            # receive RPi name and frame from the RPi and acknowledge the receipt
            _, frame = self.image_hub.recv_image()
            print('Connected and received frame at time: ' + str(datetime.now()))

            # resize the frame to have a width of IMAGE_WIDTH pixels, then
            # grab the frame dimensions and construct a blob
            frame = imutils.resize(frame, width=IMAGE_WIDTH)

            #check if stopping frame
            if self._is_stopping_frame(frame):
                restart = self._show_all_images()

                if restart:
                    self._initialise_directories()
                    self.frame_list.clear()
                else:
                    break  # stop image processing server

            # Acquire frame and expand frame dimensions to have shape: [1, None, None, 3]
            # i.e. a single-column array, where each item in the column has the pixel RGB value
            # for input into model
            #frame_expanded = np.expand_dims(frame, axis=0)

            # save raw image
            # form image file path for saving
            raw_image_name = RAW_IMAGE_PREFIX + str(count) + IMAGE_ENCODING
            raw_image_path = os.path.join(self.raw_image_dir_path, raw_image_name)
            # save raw image
            save_success = cv2.imwrite(raw_image_path, frame)
            count+=1
            print('save', raw_image_name, 'successful?', save_success)
            
            
            #import saved raw image
            #frame = cv2.imread(raw_image_path)
            image = Image.open(raw_image_path)
            
            #image = image.transpose(Image.FLIP_LEFT_RIGHT)
            predictions = self.od_model.predict_image(image)

            bounding_boxes, classes, scores = [], [], []
            
            processed_image = Image.open('stop_image_processing.png')
            for i in predictions:
                if(i["probability"] > MIN_CONFIDENCE_THRESHOLD):
                    bounding_boxes.append([i["boundingBox"]["top"]*image.size[1],
                                           i["boundingBox"]["left"]*image.size[0],
                                           (i["boundingBox"]["top"] +
                                            i["boundingBox"]["height"])*image.size[1],
                                           (i["boundingBox"]["width"] +
                                            i["boundingBox"]["left"])*image.size[0],
                                           ])
                    scores.append(i["probability"])
                    classes.append(int(i["tagName"]))
                    # Draw the results of the detection
                    processed_image = self.drawBB(i, image)
        
            
            
            obstacle_symbol_map = self._get_obstacle_map(bounding_boxes, classes, scores)

            # forms 'LEFT_SYMBOL|MIDDLE_SYMBOL|RIGHT_SYMBOL'
            return_string = '|'.join(obstacle_symbol_map.values())

            # form image file path for saving
            processed_image_name = PROCESSED_IMAGE_PREFIX + \
                str(len(self.frame_list)) + IMAGE_ENCODING
            processed_image_path = os.path.join(
                self.processed_image_dir_path, 
                processed_image_name
            )
                        
            print(return_string)
            self.image_hub.send_reply(return_string)
            # send_reply disconnects the connection
            print('Sent reply and disconnected at time: ' + str(datetime.now()) + '\n')

            if return_string != '-1|-1|-1':
                # save processed image
                processed_image.save(processed_image_path)
                self.frame_list.append(processed_image_path)

        self.end()

    def end(self):
        print('Stopping image processing server')

        self.image_hub.send_reply('End')
        # send_reply disconnects the connection
        print('Sent reply and disconnected at time: ' + str(datetime.now()) + '\n')


            
