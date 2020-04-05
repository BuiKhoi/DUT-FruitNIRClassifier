import numpy as np

def input_norm(sequence):
    maxx = sequence.max()
    return sequence / maxx

def calc_deriative(sequence):
    deriative = np.empty(len(sequence) - 1)
    for i in range(len(deriative)):
        deriative[i] = sequence[i + 1] - sequence[i]
    return deriative