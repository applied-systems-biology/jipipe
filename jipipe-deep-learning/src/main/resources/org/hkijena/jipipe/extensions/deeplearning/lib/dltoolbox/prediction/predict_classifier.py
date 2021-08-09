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

import os
import numpy as np
from glob import glob
import pandas as pd
import tensorflow as tf
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
    normalization_mode = config['normalization']
    model_img_shape = tuple(model_config["image_shape"])

    # Load the model
    if model is not None:
        assert isinstance(model, tf.keras.models.Model)
        print(f'[Predict] Use model with input shape: {model.input_shape} and output shape: {model.output_shape}')
    else:
        model = utils.load_and_compile_model(model_config, input_model_path, model)
        print(f'[Predict] Model successfully loaded from path: {input_model_path}')

    # read the input and label images in dependence of their specified format: directory or .csv-table
    X = utils.read_images(path_dir=input_dir,
                          model_input_shape=model_img_shape,
                          read_input=True,
                          labels_for_classifier=False)

    print('[Predict] Images to predict:', len(X))

    assert len(X) > 0, "No images found"

    # validate input data
    x = utils.validate_image_shape(model_img_shape, images=X)

    print('[Predict] Input data:', x.shape)

    # Preprocessing of the input data (normalization)
    print('[Predict] Input image intensity min-max-range before preprocessing:', x.min(), x.max())
    if x.max() > 1:
        x = utils.preprocessing(x, mode=normalization_mode)
        print('[Predict] Input image intensity min-max-range after preprocessing:', x.min(), x.max())

    # get filenames of input images
    read_as_images = not str(input_dir).endswith('csv')
    if read_as_images:
        filenames = np.sort(glob(input_dir))
    else:
        filenames = np.sort(pd.read_csv(input_dir, index_col=0)['input']).tolist()

    results = []
    results_columns = None

    for idx, (image, file_name) in enumerate(zip(x, filenames)):
        print(f"[Predict] [ {idx + 1} / {len(X)} ] read image with shape: {image.shape} from path: {file_name}")

        if len(image.shape) == 3:
            image = np.expand_dims(image, axis=0)

        prediction = model.predict(image, batch_size=1)

        # extract the column names from the first sample
        if results_columns is None:
            results_columns = ['input']
            results_columns.extend(['probability_class_{0}'.format(idx) for idx, elem in enumerate(prediction[0])])

        print(f"[Predict] probabilities per class: {prediction[0]}")

        # extract just first prediction, cause only one image will be predicted - possibility for speed-up (batch-size)
        tmp_results = [file_name]
        tmp_results.extend(prediction[0])

        results.append(tmp_results)

    df_results = pd.DataFrame(results, columns=results_columns)

    print(f"[Predict] all samples with their corresponding probabilities per class:\n\n{df_results}")

    # create save directory if necessary
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        print('[Predict] create directory folder for predicted images:', output_dir)

    if output_dir:
        predicted_file_name = os.path.join(output_dir, 'predictions.csv')
        print("[Predict] Saving prediction result to " + str(predicted_file_name))
        df_results.to_csv(predicted_file_name)

    return df_results
