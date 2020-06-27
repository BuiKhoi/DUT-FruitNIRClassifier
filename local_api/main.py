from fastapi import FastAPI
from keras.models import *
import numpy as np
import tensorflow as tf
from keras.backend.tensorflow_backend import set_session
import json
import os
from datetime import datetime
from common_processing import *
import math
from scipy import signal
from pydantic import BaseModel

MODEL_PATH = './models/fruit_classify_der_resnet.json'
WEIGHT_PATH = './models/fruit_classify_3_layers_resnet.h5'

BACKUP_MODEL_PATH = './models/modeleng2.json'
BACKUP_MODEL_WEIGHT_PATH = './models/modeleng2_score.h5'

LABEL_DICT_PATH = './label_dict.json'

sess = tf.Session()
set_session(sess)

app = FastAPI()

try:
    with open(MODEL_PATH, 'r') as model_file:
        run_model = model_from_json(model_file.read())
    run_model.load_weights(WEIGHT_PATH)
except Exception:
    run_model = load_model(WEIGHT_PATH)

try:
    with open(BACKUP_MODEL_PATH, 'r') as model_file:
        backup_model = model_from_json(model_file.read())
    backup_model.load_weights(BACKUP_MODEL_WEIGHT_PATH)
except Exception:
    backup_model = load_model(BACKUP_MODEL_WEIGHT_PATH)

graph = tf.get_default_graph()

with open(LABEL_DICT_PATH, 'r') as label_dict_file:
    label_dict = json.loads(label_dict_file.read())

class Data(BaseModel):
   intensity: str

def parse_intensity(intensity):
    intensity = intensity.split('x')
    intensity = np.array([float(inten) for inten in intensity])
    intensity = preprocess_spectrum(intensity, 224)
    return intensity
    
@app.post('/predict_cong/')
def predict_fruit(data:Data):
    global graph
    global sess
    global backup_model
    global label_dict
    # print(data.intensity)
    with graph.as_default():
        set_session(sess)
        intensity=parse_intensity2(data.intensity)
        X_test0=processdata(intensity)
        X_test1=processdata1(intensity)
        X_test2=processdata2(intensity)
        prediction = backup_model.predict([X_test0,X_test1,X_test2])
    print('Confidence: {}'.format(calculate_confidence(prediction)))
    label = label_dict[np.argmax(prediction)]
    return label_dict[np.argmax(prediction)]

@app.post('/predict_khoi/')
def predict_fruit(data:Data):
    global graph
    global sess
    global run_model
    global label_dict
    # print(data.intensity)
    with graph.as_default():
        set_session(sess)
        prediction = run_model.predict(np.expand_dims(parse_intensity(data.intensity), 0))
    label = label_dict[np.argmax(prediction)]
    return label_dict[np.argmax(prediction)]

@app.post('/collect/')
def collect_data(
    intensity: str = None, 
    fruit_type: str = None,
    measure_place: int = None,
    measure_index: int = None,
    sensor_id: int = None,
    bought_date: str = None,
    sub_type: int = None
):
    save_path = './collected_data/' + fruit_type + '/'
    if not os.path.exists(save_path):
        os.mkdir(save_path)
    inten = parse_intensity(intensity)
    file_name = str(datetime.now()).split('.')[0]
    measure_atts = {}
    measure_atts['fruit_type'] = fruit_type
    measure_atts['measure_place'] = measure_place
    measure_atts['measure_index'] = measure_index
    measure_atts['sensor_id'] = sensor_id
    measure_atts['bought_date'] = bought_date
    measure_atts['sub_type'] = sub_type
    with open(save_path + file_name + '.json', 'w') as attribute_file:
        attribute_file.write(json.dumps(measure_atts))
    np.save(save_path + file_name, inten)
    return 'saved'