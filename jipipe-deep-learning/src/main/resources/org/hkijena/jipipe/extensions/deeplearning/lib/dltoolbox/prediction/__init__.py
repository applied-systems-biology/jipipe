#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""

@author: J-P Praetorius
@email: jan-philipp.praetorius@leibniz-hki.de or p.e.mueller07@gmail.com

Copyright by Jan-Philipp Praetorius

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
https://www.leibniz-hki.de/en/applied-systems-biology.html
HKI-Center for Systems Biology of Infection
Leibniz Institute for Natural Product Research and Infection Biology -
Hans Knöll Insitute (HKI)
Adolf-Reichwein-Straße 23, 07745 Jena, Germany

Script to predict a network
"""

import math
import os
from pathlib import Path

import numpy as np
import tifffile
from skimage import img_as_float32
from skimage import io

from dltoolbox.utils import load_and_compile_model, sliding_window


def predict(model_config, config, model=None):
    """
    Predicts a model with some input data
    Args:
        config: the config for this prediction
        model_config: the model config
        model: An existing model (optional)

    Returns: Prediction results (list of predictions, list of input files)

    """

    input_dir = config['input_dir']
    output_dir = config['output_dir']
    input_model_path = config["input_model_path"] if "input_model_path" in config else model_config['output_model_path']
    model_img_shape = tuple(model_config["image_shape"])

    # Load the model
    model = load_and_compile_model(model_config, input_model_path, model)

    # Load the raw images
    X = io.imread_collection(input_dir)
    print('[Predict] Images to predict:', len(X))

    results = []

    for (image, file_name) in zip(X, X.files):
        print("[Predict] " + str(file_name))

        img_x0 = image.shape[0]
        img_x1 = image.shape[1]

        print("[Predict] Image shape is " + str(image.shape))

        try:
            print("[Predict] Attempting to predict whole image")
            whole_image = image
            while len(whole_image.shape) < 4:
                whole_image = np.expand_dims(whole_image, axis=0)
            prediction = model.predict(whole_image, batch_size=1)
        except:
            print("[Predict] Predicting the whole image failed. Retrying with tiling.")
            ws0 = model_img_shape[0]
            ws1 = model_img_shape[1]
            print("[Predict] Window size is " + str((ws0, ws1)))

            # First pad the image to something perfectly tiling
            if img_x0 % ws0 != 0 or img_x1 % ws1 != 0:
                print("[Predict] Padding is required with image of shape " + str(image.shape) +
                      " and window size " + str([ws0, ws1]))
                pad_width = [(0, int(math.ceil(1.0 * img_x0 / ws0) * ws0) - img_x0),
                             (0, int(math.ceil(1.0 * img_x1 / ws1) * ws1) - img_x1)]
                while len(pad_width) < len(image.shape):
                    pad_width.append((0, 0))
                print("[Predict] Padding with " + str(pad_width))
                img_padded = np.pad(image, pad_width=pad_width)
                print("[Predict] Padded image has shape " + str(img_padded.shape))
            else:
                print("[Predict] No padding required with image of shape " + str(image.shape) +
                      " and window size " + str([ws0, ws1]))
                img_padded = image

            prediction = None
            for (window_x0, window_x1, img_window) in sliding_window(img_padded,
                                                                   step_size=(ws0, ws1),
                                                                   window_size=(ws0, ws1)):
                print("[Predict] Predicting window " + str((window_x0, window_x1, ws0, ws1))
                      + " in image " + str(img_padded.shape))
                img_window_expanded = img_window

                while len(img_window_expanded.shape) < 4:
                    img_window_expanded = np.expand_dims(img_window_expanded, axis=0)

                window_prediction = img_as_float32(model.predict_on_batch([img_window_expanded]))
                window_prediction = np.squeeze(window_prediction)

                new_x0 = min(ws0, img_x0 - window_x0)
                new_x1 = min(ws1, img_x1 - window_x1)
                if new_x0 != ws0 or new_x1 != ws1:
                    window_prediction = window_prediction[0:new_x0, 0:new_x1]

                if prediction is None:
                    # Generate output image
                    prediction_shape = list(window_prediction.shape)
                    prediction_shape[0] = img_x0
                    prediction_shape[1] = img_x1
                    print("[Predict] Initializing output image with shape " + str(prediction_shape))
                    prediction = np.zeros(prediction_shape, dtype=np.float32)

                # Add window into output
                prediction[window_x0:window_x0 + window_prediction.shape[0],
                    window_x1:window_x1 + window_prediction.shape[1]] = window_prediction

        # Postprocessing
        prediction = np.squeeze(prediction)
        prediction = img_as_float32(prediction)

        if output_dir:
            predicted_file_name = Path(output_dir) / os.path.basename(file_name)
            print("[Predict] Saving prediction result to " + str(predicted_file_name))
            if str(file_name).endswith(".tif") or str(file_name).endswith(".tiff"):
                tifffile.imsave(predicted_file_name, prediction)
            else:
                io.imsave(predicted_file_name, prediction)

        results.append(prediction)

    return results, X.files
