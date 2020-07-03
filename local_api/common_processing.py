import numpy as np
import cv2
from scipy.signal import savgol_filter
import math
from scipy import signal

def calc_standar_scale(spectrum, resize_shape = None):
    std = spectrum.std()
    mean = std.mean()
    new_spectrum = (spectrum - mean)/std
    if resize_shape is None:
        return new_spectrum
    else:
        return cv2.resize(new_spectrum, resize_shape)

def preprocess_spectrum(spectrum, output_size = 224):
    output_shape = (1, 224)
    spectrum = calc_standar_scale(spectrum, output_shape)
    fir_der = savgol_filter(spectrum[:, 0], 25, polyorder = 5, deriv=1)
    sec_der = savgol_filter(spectrum[:, 0], 25, polyorder = 5, deriv=2)

    pp_spectrum = np.concatenate([
        spectrum,
        calc_standar_scale(fir_der, output_shape),
        calc_standar_scale(sec_der, output_shape)
    ], 1)

    # print(pp_spectrum.shape)

    return pp_spectrum

def parse_intensity2(intensity):
    intensity = intensity.split('x')
    intensity = np.array([float(inten) for inten in intensity])
    intensity=np.expand_dims(intensity, axis=0)
    return intensity

def smooth1(x):
  X_new=x.reshape(228,)
  y=signal.savgol_filter(X_new,window_length=25, polyorder=5,deriv=1)
  y=np.expand_dims(y,1)
  y=np.asarray(y)
  return y

def smooth2(x):
  X_new=x.reshape(228,)
  y=signal.savgol_filter(X_new,window_length=25, polyorder=5,deriv=2)
  y=np.expand_dims(y,1)
  y=np.asarray(y)
  return y

def standardd(X):
    Xnew=np.asarray(X,dtype='float')
    total=0
    u=X.sum()/len(X)
    for i in range(len(X)):
        total+=((X[i]-u)**2)
    o=math.sqrt(total/len(X))
    for i in range(len(X)):
        Xnew[i]=float((X[i]-u)/o)
    return Xnew

def processdata(X_data):
  X_f=[]
  for i in range(X_data.shape[0]):
    X_data[i]=standardd(X_data[i])
    xtem=X_data[i].reshape(228,)
    X_f.append(cv2.resize(xtem,(1,224)))
  X_f=np.asarray(X_f)
  print(X_f.shape)
  return X_f

def processdata1(X_data):
  for i in range(X_data.shape[0]):
          X_data[i]=standardd(X_data[i])
  X_f=[]
  X_tem=np.asarray(list(map(lambda e: smooth1(e),X_data)))
  for i in range(X_tem.shape[0]):
    xtem=X_tem[i].reshape(228,)
    X_f.append(cv2.resize(xtem,(1,224)))
  X_f=np.asarray(X_f)
  print(X_f.shape)
  return X_f
  
def processdata2(X_data):
  for i in range(X_data.shape[0]):
    X_data[i]=standardd(X_data[i])
  X_f=[]
  X_tem=np.asarray(list(map(lambda e: smooth2(e),X_data)))
  for i in range(X_tem.shape[0]):
    xtem=X_tem[i].reshape(228,)
    X_f.append(cv2.resize(xtem,(1,224)))
  X_f=np.asarray(X_f)
  print(X_f.shape)
  return X_f

def calculate_confidence(prediction):
  total_sum = np.sum(prediction)
  maxx = np.max(prediction)

  return maxx / total_sum