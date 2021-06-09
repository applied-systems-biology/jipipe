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

from dltoolbox.utils import load_and_compile_model
from skimage import io
from skimage import img_as_float32
import tifffile
import os
import numpy as np
from pathlib import Path
from skimage.transform import resize


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
    img_size = model_config["img_size"]

    # Load the model
    model = load_and_compile_model(model_config, input_model_path, model)

    # Load the raw images
    X = io.imread_collection(input_dir)
    print('[Predict] Images to predict:', len(X))

    results = []

    for (image, file_name) in zip(X, X.files):
        print("[Predict] " + str(file_name))

        # Downscaling (if needed)
        if image.shape[0] != img_size or image.shape[1] != img_size:
            target_shape = list(image.shape)
            target_shape[0] = target_shape[1] = img_size
            target_shape = tuple(target_shape)
            print("[Predict] Resizing image from " + str(image.shape) + " to " + str(target_shape))
            image = resize(image, target_shape)

        if len(image.shape) == 3:
            image = np.expand_dims(image, axis=0)

        prediction = model.predict_on_batch([image])
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
