import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
import keras
import keras.backend as K
from keras import optimizers
from keras.callbacks import *
from keras.models import *
from sklearn.metrics import confusion_matrix

from models import *

LOAD_MODEL = False
LOAD_MODEL_PATH = './Checkpoints/Weight/fruit_classify_3_layers_resnet.h5'
MODEL_SAVE_PATH = './Checkpoints/Weight/fruit_classify_3_layers_resnet.h5'
MODEL_CONFIG_SAVE_PATH = './Checkpoints/Model/fruit_classify_3_layers_resnet.json'
if LOAD_MODEL:
    train_model = load_model(LOAD_MODEL_PATH)
else:
    train_model = resnet_based_model()

train_model.summary()

adam = optimizers.Adam(0.0001, 0.9, 0.99)
train_model.compile(adam, 'categorical_crossentropy', ['accuracy'])
reduce_lr = callbacks.ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=30, min_lr=0.0001, verbose = 1)
checkpoint = callbacks.ModelCheckpoint(MODEL_SAVE_PATH, 'val_accuracy', 1, True)
early_stop = callbacks.EarlyStopping('val_loss', patience=200, verbose=1, restore_best_weights=True)

temp = np.load('./Data/Training data/training_3layers.npz')
X_data = temp['xdata']
y_true = temp['ytrue']
y_hot = np.zeros((len(y_true), y_true.max() + 1))
y_hot[np.arange(len(y_true)), y_true] = 1
X_train, X_test, y_train, y_test = train_test_split(X_data, y_hot)

history = train_model.fit(X_train, y_train, 128, 3000, 1, [reduce_lr, checkpoint, early_stop], validation_data=(X_test, y_test))
train_model.load_weights(MODEL_SAVE_PATH)

# plt.plot(history.history['val_accuracy'])
# plt.show()
print('Best accuracy: {}'.format(max(history.history['val_accuracy'])))

plt.plot(history.history['val_accuracy'])
plt.title('Val accuracy')
plt.xlabel('Epochs')
plt.show()

plt.plot(history.history['val_loss'])
plt.title('Val loss')
plt.xlabel('Epochs')
plt.show()

y_pred = train_model.predict(X_test)
y_p = [np.argmax(yy) for yy in y_pred]
y_tp = [np.argmax(yy) for yy in y_test]
print(confusion_matrix(y_tp, y_p))
plt.imshow(confusion_matrix(y_tp, y_p), 'gray')
plt.show()

with open(MODEL_CONFIG_SAVE_PATH, 'w') as model_write:
    model_write.write(train_model.to_json())
train_model.save(MODEL_SAVE_PATH)