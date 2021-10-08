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

Script to create a VGG16 model
"""

import tensorflow as tf
from dltoolbox import utils

def build_model(config):
    """
    Creates a VGG16 model
    Args:
        config: the model parameters

    Returns: the model

    """

    img_shape = tuple(config["image_shape"])
    num_classes = config['n_classes']
    model_path = config['output_model_path']
    model_json_path = config["output_model_json_path"]

    def secondConvBlock(input_tensor, num_filters):
        sec_conv = tf.keras.layers.Conv2D(num_filters, (3, 3), padding='same')(input_tensor)
        sec_conv = tf.keras.layers.Conv2D(num_filters, (3, 3), padding='same')(sec_conv)
        sec_conv = tf.keras.layers.BatchNormalization()(sec_conv)
        sec_conv = tf.keras.layers.Activation('relu')(sec_conv)
        sec_conv = tf.keras.layers.MaxPooling2D((2, 2))(sec_conv)
        return sec_conv

    def thirdConvBlock(input_tensor, num_filters):
        thi_conv = tf.keras.layers.Conv2D(num_filters, (3, 3), padding='same')(input_tensor)
        thi_conv = tf.keras.layers.Conv2D(num_filters, (3, 3), padding='same')(thi_conv)
        thi_conv = tf.keras.layers.Conv2D(num_filters, (3, 3), padding='same')(thi_conv)
        thi_conv = tf.keras.layers.BatchNormalization()(thi_conv)
        thi_conv = tf.keras.layers.Activation('relu')(thi_conv)
        thi_conv = tf.keras.layers.MaxPooling2D((2, 2))(thi_conv)
        return thi_conv

    inputs = tf.keras.layers.Input(shape=img_shape)

    # 2nd - convolution - block
    sCB_0 = secondConvBlock(inputs, num_filters=64)
    sCB_1 = secondConvBlock(sCB_0, num_filters=128)

    # 3rd - convolution - block
    tCB_2 = thirdConvBlock(sCB_1, num_filters=256)
    tCB_3 = thirdConvBlock(tCB_2, num_filters=512)
    tCB_4 = thirdConvBlock(tCB_3, num_filters=512)

    # flatten the last convolutional layer
    flatten = tf.keras.layers.Flatten()(tCB_4)

    # dense-layers for classification
    dense_5 = tf.keras.layers.Dense(4096, activation='relu')(flatten)
    dropout_6 = tf.keras.layers.Dropout(0.5)(dense_5)

    dense_7 = tf.keras.layers.Dense(4096, activation='relu')(dropout_6)
    dropout_8 = tf.keras.layers.Dropout(0.5)(dense_7)

    # last layer for classification
    output = tf.keras.layers.Dense(num_classes, activation='softmax')(dropout_8)

    # create the model
    model = tf.keras.models.Model(inputs=[inputs], outputs=[output])

    # compile model
    if num_classes == 2:
        model.compile(loss='binary_crossentropy', optimizer=tf.keras.optimizers.Adam(), metrics=['acc'])
    else:
        model.compile(loss='categorical_crossentropy', optimizer=tf.keras.optimizers.Adam(), metrics=['acc'])

    model.summary()

    # save the model, model-architecture and model-config
    utils.save_model_with_json(model=model,
                               model_path=model_path,
                               model_json_path=model_json_path,
                               model_config=config,
                               operation_config=None)

    return model
