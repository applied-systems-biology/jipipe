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

Script to create a SegNet model
"""


import tensorflow as tf


def build_model(config):
    """
    Build FCN32 classification model

    Args:
        config: The model parameters

    Returns: The model
    """

    img_shape = tuple(config["image_shape"])
    n_classes = config['n_classes']
    model_path = config['output_model_path']

    model_json_path = config["output_model_json_path"]

    img_input = tf.keras.layers.Input(shape=img_shape)

    model = tf.keras.applications.vgg16.VGG16(include_top=False, input_tensor=img_input)

    assert isinstance(model, tf.keras.models.Model)

    o = tf.keras.layers.Conv2D(filters=4096, kernel_size=(7, 7), padding="same", activation="relu", name="fc6")(model.output)
    o = tf.keras.layers.Dropout(rate=0.5)(o)
    o = tf.keras.layers.Conv2D(filters=4096, kernel_size=(1, 1), padding="same", activation="relu", name="fc7")(o)
    o = tf.keras.layers.Dropout(rate=0.5)(o)

    o = tf.keras.layers.Conv2D(filters=n_classes, kernel_size=(1, 1), padding="same", activation="relu",
                            kernel_initializer="he_normal", name="score_fr")(o)

    o = tf.keras.layers.Conv2DTranspose(filters=n_classes, kernel_size=(32, 32), strides=(32, 32), padding="valid",
                               activation=None,
                               name="score2")(o)

    o = tf.keras.layers.Reshape((-1, n_classes))(o)
    o = tf.keras.layers.Activation("softmax")(o)

    fcn8 = tf.keras.models.Model(inputs=img_input, outputs=o)

    fcn8.summary()

    if model_path:
        fcn8.save(model_path)
        print('[Create model] Saved model to:', model_path)

    if model_json_path:
        model_json = fcn8.to_json()
        with open(model_json_path, "w") as f:
            f.write(model_json)
        print('[Create model] Saved model JSON to:', model_json_path)

    return fcn8
