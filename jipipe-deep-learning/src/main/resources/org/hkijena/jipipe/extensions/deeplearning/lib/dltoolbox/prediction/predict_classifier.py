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


import numpy as np
import pandas as pd
import math
import os
from pathlib import Path
import tifffile
from skimage import io
import sys
from dltoolbox import utils


def predict_samples(model_config, config, model=None):
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

    # Load the model
    model = utils.load_and_compile_model(model_config, input_model_path, model)

    # Load the raw images
    X = utils.imread_collection(input_dir)
    print('[Predict] Images to predict:', len(X))

    results = []
    results_columns = None

    for (image, file_name) in zip(X, X.files):
        print(f"[Predict] read image with shape {image.shape} from path: " + str(file_name))

        # Preprocessing (normalization)
        image = utils.preprocessing(image, mode=config['normalization'])

        while len(image.shape) < 4:
            image = np.expand_dims(image, axis=-1)
            image = np.expand_dims(image, axis=0)

        prediction = model.predict_on_batch(image)

        # extract the column names from the first sample
        if results_columns is None:
            results_columns = ['sample']
            results_columns.extend(['probability_class_{0}'.format(idx) for idx, elem in enumerate(prediction[0])])

        print(f"[Predict] probabilities per class: {prediction[0]}")

        # extract just first prediction, cause only one image will be predicted - possibility for speed-up (batch-size)
        tmp_results = [file_name]
        tmp_results.extend(prediction[0])

        results.append(tmp_results)

    df_results = pd.DataFrame(results, columns=results_columns)

    print(f"[Predict] all samples with their corresponding probabilities per class:\n\n{df_results}")

    if output_dir:
        predicted_file_name = os.path.join(Path(output_dir), 'predictions.csv')
        print("[Predict] Saving prediction result to " + str(predicted_file_name))
        df_results.to_csv(predicted_file_name)

    return df_results
