import os
from keras.layers import Conv1D, MaxPool1D, Dropout, UpSampling1D, Input, Flatten, Dense, LeakyReLU
from keras.models import *
from keras.layers.merge import concatenate

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

def basc_cnn_model(input_shape = (224, 1), classes = 5):
    model = Sequential()

    model.add(Conv1D(16, 5, activation = 'relu', input_shape = input_shape))
    model.add(MaxPool1D(2, 2))
    model.add(Conv1D(32, 5, activation = 'relu'))
    model.add(MaxPool1D(2, 2))
    model.add(Conv1D(64, 5, activation = 'relu'))
    model.add(MaxPool1D(2, 2))

    model.add(Flatten())

    model.add(Dense(128, activation='relu'))
    # model.add(Dropout(0.3))
    model.add(Dense(512, activation='relu'))
    # model.add(Dropout(0.3))
    model.add(Dense(classes, activation='softmax'))
    return model

def resnet_based_model(input_shape = (224, 3), classes = 5):
    inpt = Input(input_shape)
    
    conv1 = Conv1D(16, 3, padding='same')(inpt)
    conv1 = LeakyReLU()(conv1)
    conv1 = concatenate([inpt, conv1], 2)
    conv1 = Conv1D(32, 3, padding='same')(conv1)
    conv1 = LeakyReLU()(conv1)
    pool1 = MaxPool1D(2, 2)(conv1)
    
    conv2 = Conv1D(32, 3, padding='same')(pool1)
    conv2 = LeakyReLU()(conv2)
    conv2 = concatenate([pool1, conv2], 2)
    conv2 = Conv1D(64, 3, padding='same')(conv2)
    conv2 = LeakyReLU()(conv2)
    pool2 = MaxPool1D(2, 2)(conv2)
    
    conv3 = Conv1D(16, 3, padding='same')(pool2)
    conv3 = LeakyReLU()(conv3)
    conv3 = concatenate([pool2, conv3], 2)
    conv3 = Conv1D(32, 3, padding='same')(conv3)
    conv3 = LeakyReLU()(conv3)
    pool3 = MaxPool1D(2, 2)(conv3)
    
    conv4 = Conv1D(32, 3, padding='same')(pool3)
    conv4 = LeakyReLU()(conv4)
    conv4 = concatenate([pool3, conv4], 2)
    conv4 = Conv1D(64, 3, padding='same')(conv4)
    conv4 = LeakyReLU()(conv4)
    pool4 = MaxPool1D(2, 2)(conv4)
    
    flatt = Flatten()(pool4)
    dens0 = Dense(128)(flatt)
    dens0 = LeakyReLU()(dens0)
    drop0 = Dropout(0.5)(dens0)
    
    dens1 = Dense(256)(drop0)
    dens1 = LeakyReLU()(dens1)
    drop1 = Dropout(0.5)(dens1)
    outpt = Dense(classes, activation='softmax')(drop1)
    
    return Model(inpt, outpt)