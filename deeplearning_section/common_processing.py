import numpy as np
import cv2
from scipy.signal import savgol_filter

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