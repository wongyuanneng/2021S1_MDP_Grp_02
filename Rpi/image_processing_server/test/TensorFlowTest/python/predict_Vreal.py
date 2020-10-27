# The steps implemented in the object detection sample code:
# 1. for an image of width and height being (w, h) pixels, resize image to (w', h'), where w/h = w'/h' and w' x h' = 262144
# 2. resize network input size to (w', h')
# 3. pass the image to network and do inference
# (4. if inference speed is too slow for you, try to make w' x h' smaller, which is defined with DEFAULT_INPUT_SIZE (in object_detection.py or ObjectDetection.cs))
"""Sample prediction script for TensorFlow 2.x."""
import sys
import tensorflow as tf
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from object_detection import ObjectDetection

# from IPython.display import Image


MODEL_FILENAME = 'model.pb'
LABELS_FILENAME = 'labels.txt'

cwd_path = os.getcwd()


# class ImageProcessingServer:
#     def __init__(self):
#         self.inference_graph_path = os.path(cwd_path, MODEL_FILENAME)

#         # self._initialise_directories()
#         detection_graph = tf.Graph()
#         with tf.io.gfile.GFile(self.inference_graph_path, 'rb') as fid:
#             serialized_graph = fid.read()
#             od_graph_def.ParseFromString(serialized_graph)
#             tf.import_graph_def(od_graph_def, name='')

#         self.session = tf.compat.v1.Session(graph=detection_graph)


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


def main(image_filename):
    image_hub = imagezmq.CustomImageHub()
    stopping_image = Image.open("stop_image_processing.png")
    graph_def = tf.compat.v1.GraphDef()
    with tf.io.gfile.GFile(MODEL_FILENAME, 'rb') as f:
        graph_def.ParseFromString(f.read())
    with open(LABELS_FILENAME, 'r') as f:
        labels = [l.strip() for l in f.readlines()]

    od_model = TFObjectDetection(graph_def, labels)

    print('\nStarted image processing server.\n')

    while True:
        print('Waiting for image from RPi...')
        _, frame = self.image_hub.recv_image()
        print('Connected and received frame at time: ' + str(datetime.now())
        frame=imutils.resize(frame, width=1920)
        if(cv2.subtract(frame, stopping_image))


    image=Image.open(image_filename)
    print(image)
    print(np.asarray(image).shape)
    predictions=od_model.predict_image(image)
    # print("open")
    # print(image_filename)
    # print(image.size)
    print(predictions[0])
    font=ImageFont.truetype("arial.ttf", 30)
    shape=[predictions[0]["boundingBox"]["left"]*image.size[0],
             predictions[0]["boundingBox"]["top"]*image.size[1],
             (predictions[0]["boundingBox"]["width"] +
              predictions[0]["boundingBox"]["left"])*image.size[0],
             (predictions[0]["boundingBox"]["top"] +
              predictions[0]["boundingBox"]["height"])*image.size[1]]

    img1=ImageDraw.Draw(image)
    img1.text((shape[0], shape[1]), "id:"+predictions[0]["tagName"] + " |Acc:" +
              str(predictions[0]["probability"]), font=font)
    img1.rectangle(shape, outline="red")
    image.show()


if __name__ == '__main__':
    if len(sys.argv) <= 1:
        print('USAGE: {} image_filename'.format(sys.argv[0]))
    else:
        main(sys.argv[1])
