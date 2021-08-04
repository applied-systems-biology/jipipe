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

Script to create a Xception model
"""

# copied from:
# https://raw.githubusercontent.com/yanchummar/xception-keras/master/xception_model.py
# alternative:
"""
tf.keras.applications.xception.Xception(
    include_top=True, weights='imagenet', input_tensor=None,
    input_shape=None, pooling=None, classes=1000,
    classifier_activation='softmax'
)
"""

import tensorflow as tf
from dltoolbox import utils
from dltoolbox.models import metrics


def build_model(config):
    """
    Creates a Xception model
    Args:
        config: the model parameters

    Returns: the model

    """

    img_shape = tuple(config["image_shape"])
    num_classes = config['n_classes']
    model_path = config['output_model_path']
    model_json_path = config["output_model_json_path"]
    model_type = config['model_type']
    learning_rate = config['learning_rate']

    inputs = tf.keras.layers.Input(shape=img_shape)

    # Block 1
    x = tf.keras.layers.Conv2D(32, (3, 3), strides=(2, 2), use_bias=False)(inputs)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.Conv2D(64, (3, 3), use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)

    residual = tf.keras.layers.Conv2D(128, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = tf.keras.layers.BatchNormalization()(residual)

    # Block 2
    x = tf.keras.layers.SeparableConv2D(128, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.SeparableConv2D(128, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)

    # Block 2 Pool
    x = tf.keras.layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = tf.keras.layers.add([x, residual])

    residual = tf.keras.layers.Conv2D(256, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = tf.keras.layers.BatchNormalization()(residual)

    # Block 3
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.SeparableConv2D(256, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.SeparableConv2D(256, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)

    # Block 3 Pool
    x = tf.keras.layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = tf.keras.layers.add([x, residual])

    residual = tf.keras.layers.Conv2D(728, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = tf.keras.layers.BatchNormalization()(residual)

    # Block 4
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)

    x = tf.keras.layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = tf.keras.layers.add([x, residual])

    # Block 5 - 12
    for i in range(8):
        residual = x

        x = tf.keras.layers.Activation('relu')(x)
        x = tf.keras.layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
        x = tf.keras.layers.BatchNormalization()(x)
        x = tf.keras.layers.Activation('relu')(x)
        x = tf.keras.layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
        x = tf.keras.layers.BatchNormalization()(x)
        x = tf.keras.layers.Activation('relu')(x)
        x = tf.keras.layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
        x = tf.keras.layers.BatchNormalization()(x)

        x = tf.keras.layers.add([x, residual])

    residual = tf.keras.layers.Conv2D(1024, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = tf.keras.layers.BatchNormalization()(residual)

    # Block 13
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)
    x = tf.keras.layers.SeparableConv2D(1024, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)

    # Block 13 Pool
    x = tf.keras.layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = tf.keras.layers.add([x, residual])

    # Block 14
    x = tf.keras.layers.SeparableConv2D(1536, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)

    # Block 14 part 2
    x = tf.keras.layers.SeparableConv2D(2048, (3, 3), padding='same', use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Activation('relu')(x)

    # Fully Connected Layer
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(num_classes, activation='softmax')(x)

    # Create model
    model = tf.keras.models.Model(inputs, x, name='xception')

    # Download and cache the Xception weights file
    # weights_path = get_file('xception_weights.h5', WEIGHTS_PATH, cache_subdir='models')
    # load weights
    # model.load_weights(weights_path)

    # get all metrics
    model_metrics = metrics.get_metrics(model_type, num_classes)

    # compile model
    adam = tf.keras.optimizers.Adam(lr=learning_rate)
    if num_classes == 2:
        model.compile(loss='binary_crossentropy', optimizer=adam, metrics=model_metrics)
    else:
        model.compile(loss='categorical_crossentropy', optimizer=adam, metrics=model_metrics)

    model.summary()

    # save the model, model-architecture and model-config
    utils.save_model_with_json(model=model,
                               model_path=model_path,
                               model_json_path=model_json_path,
                               model_config=config,
                               operation_config=None)

    return model
