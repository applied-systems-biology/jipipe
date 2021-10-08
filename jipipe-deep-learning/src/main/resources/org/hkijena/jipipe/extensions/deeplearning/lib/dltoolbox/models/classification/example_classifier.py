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

    return model
