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


def build_model(config):
    """
    Creates a Xception model
    Args:
        config: the model parameters

    Returns: the model

    """

    img_shape = tuple(config["image_shape"])
    reg_method = config['regularization_method']
    reg_method_rate = config['regularization_lambda']
    num_classes = config['n_classes']

    inputs = tf.keras.layers.Input(shape=img_shape)

    # perform regularization on input / visible - layer
    if reg_method == 'Dropout':
        inputs_reg = tf.keras.layers.Dropout(reg_method_rate)(inputs)
    elif reg_method == 'GaussianDropout':
        inputs_reg = tf.keras.layers.GaussianDropout(reg_method_rate)(inputs)
    elif reg_method == 'GaussianNoise':
        inputs_reg = tf.keras.layers.GaussianNoise(reg_method_rate)(inputs)

    # distinguish for any or none regularization method
    if reg_method in ['Dropout', 'GaussianDropout', 'GaussianNoise']:
        x = tf.keras.layers.Conv2D(32, (3, 3), strides=(2, 2), use_bias=False)(inputs_reg)
    else:
        x = tf.keras.layers.Conv2D(32, (3, 3), strides=(2, 2), use_bias=False)(inputs)

    # Block 1
    # x = tf.keras.layers.Conv2D(32, (3, 3), strides=(2, 2), use_bias=False)(inputs)
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

    # perform regularization on input / visible - layer
    if reg_method == 'Dropout':
        x = tf.keras.layers.Dropout(reg_method_rate)(x)
    elif reg_method == 'GaussianDropout':
        x = tf.keras.layers.GaussianDropout(reg_method_rate)(x)
    elif reg_method == 'GaussianNoise':
        x = tf.keras.layers.GaussianNoise(reg_method_rate)(x)

    # Fully Connected Layer
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(num_classes, activation='softmax')(x)

    # Create model
    model = tf.keras.models.Model(inputs, x, name='xception')

    # Download and cache the Xception weights file
    # weights_path = get_file('xception_weights.h5', WEIGHTS_PATH, cache_subdir='models')
    # load weights
    # model.load_weights(weights_path)

    return model
