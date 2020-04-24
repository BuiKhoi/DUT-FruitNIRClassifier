from fastapi import FastAPI
from keras.models import *
import numpy as np
import tensorflow as tf
from keras.backend.tensorflow_backend import set_session
import json
import os
from datetime import datetime

MODEL_PATH = './models/fruit_classify_der_resnet.json'
WEIGHT_PATH = './models/fruit_classify_der_resnet.h5'
LABEL_DICT_PATH = './label_dict.json'

sess = tf.Session()
set_session(sess)

app = FastAPI()

with open(MODEL_PATH, 'r') as model_file:
    run_model = model_from_json(model_file.read())
run_model.load_weights(WEIGHT_PATH)
graph = tf.get_default_graph()

with open(LABEL_DICT_PATH, 'r') as label_dict_file:
    label_dict = json.loads(label_dict_file.read())

def parse_intensity(intensity):
    intensity = intensity.split('x')
    intensity = np.array([float(inten) for inten in intensity])
    return intensity

@app.post('/predict/')
def predict_fruit(intensity: str = None):
    global graph
    global sess
    global run_model
    global label_dict
    # print(intensity)
    pred_value = np.expand_dims(parse_intensity(intensity), 1)
    with graph.as_default():
        set_session(sess)
        prediction = run_model.predict(np.expand_dims(pred_value, 0))
    label = label_dict[np.argmax(prediction)]
    return label_dict[np.argmax(prediction)]

@app.post('/collect/')
def collect_data(intensity: str = None, fruit: str = None):
    save_path = './collected_data/' + fruit + '/'
    if not os.path.exists(save_path):
        os.mkdir(save_path)
    inten = parse_intensity(intensity)
    np.save(save_path + str(datetime.now()), inten)
    return 'saved'

@app.post('/test_post/')
def test_post_function(name: str = None):
    print(name)
    return None

@app.get("/")
def read_root():
    return {"Hello": "World"}


@app.get("/items/{item_id}")
def read_item(item_id: int, q: str = None):
    return {"item_id": item_id, "q": q}
