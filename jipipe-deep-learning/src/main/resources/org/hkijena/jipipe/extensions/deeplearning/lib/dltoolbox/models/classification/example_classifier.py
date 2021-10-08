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

Script to create a mobilenet model
"""

import tensorflow as tf
from dltoolbox import utils
from dltoolbox.models import metrics


def build_model(config):
    """
    Build MobileNetV2 classification model.
    Set weights='imagenet' to use a pretrained model.

    Args:
        config: The model parameters

    Returns: The model
    """

    img_shape = tuple(config["image_shape"])
    num_classes = config['n_classes']
    model_path = config['output_model_path']
    model_json_path = config["output_model_json_path"]
    model_type = config['model_type']

    # define the model
    inputs = tf.keras.layers.Input(shape=img_shape)

    x = tf.keras.layers.Conv2D(32, kernel_size=(3, 3), activation="relu")(inputs)
    x = tf.keras.layers.MaxPooling2D(pool_size=(2, 2))(x)
    x = tf.keras.layers.Conv2D(64, kernel_size=(3, 3), activation="relu")(x)
    x = tf.keras.layers.MaxPooling2D(pool_size=(2, 2))(x)
    x = tf.keras.layers.Flatten()(x)
    x = tf.keras.layers.Dropout(0.5)(x)
    x = tf.keras.layers.Dense(num_classes, activation="softmax")(x)

    model = tf.keras.models.Model(inputs=[inputs], outputs=[x])

    # get all metrics
    model_metrics = metrics.get_metrics(model_type, num_classes)

    # compile model
    model.compile(loss="categorical_crossentropy", optimizer="adam", metrics=model_metrics)

    model.summary()

    # save the model, model-architecture and model-config
    utils.save_model_with_json(model=model,
                               model_path=model_path,
                               model_json_path=model_json_path,
                               model_config=config,
                               operation_config=None)

    return model
