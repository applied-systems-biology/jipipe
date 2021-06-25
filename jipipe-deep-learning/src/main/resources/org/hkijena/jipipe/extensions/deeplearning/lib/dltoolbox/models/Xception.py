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

from keras import layers
from keras import models
from keras import optimizers

# from keras.applications.imagenet_utils import _obtain_input_shape
# from keras.utils.data_utils import get_file

# WEIGHTS_PATH = 'https://github.com/fchollet/deep-learning-models/releases/download/v0.4/xception_weights_tf_dim_ordering_tf_kernels.h5'


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

    # # Determine proper input shape
    # input_shape = _obtain_input_shape(None, default_size=299, min_size=71, data_format='channels_last',
    #                                   include_top=False)

    inputs = layers.Input(shape=img_shape)

    # Block 1
    x = layers.Conv2D(32, (3, 3), strides=(2, 2), use_bias=False)(inputs)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)
    x = layers.Conv2D(64, (3, 3), use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)

    residual = layers.Conv2D(128, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = layers.BatchNormalization()(residual)

    # Block 2
    x = layers.SeparableConv2D(128, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)
    x = layers.SeparableConv2D(128, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)

    # Block 2 Pool
    x = layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = layers.add([x, residual])

    residual = layers.Conv2D(256, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = layers.BatchNormalization()(residual)

    # Block 3
    x = layers.Activation('relu')(x)
    x = layers.SeparableConv2D(256, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)
    x = layers.SeparableConv2D(256, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)

    # Block 3 Pool
    x = layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = layers.add([x, residual])

    residual = layers.Conv2D(728, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = layers.BatchNormalization()(residual)

    # Block 4
    x = layers.Activation('relu')(x)
    x = layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)
    x = layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)

    x = layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = layers.add([x, residual])

    # Block 5 - 12
    for i in range(8):
        residual = x

        x = layers.Activation('relu')(x)
        x = layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
        x = layers.BatchNormalization()(x)
        x = layers.Activation('relu')(x)
        x = layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
        x = layers.BatchNormalization()(x)
        x = layers.Activation('relu')(x)
        x = layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
        x = layers.BatchNormalization()(x)

        x = layers.add([x, residual])

    residual = layers.Conv2D(1024, (1, 1), strides=(2, 2), padding='same', use_bias=False)(x)
    residual = layers.BatchNormalization()(residual)

    # Block 13
    x = layers.Activation('relu')(x)
    x = layers.SeparableConv2D(728, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)
    x = layers.SeparableConv2D(1024, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)

    # Block 13 Pool
    x = layers.MaxPooling2D((3, 3), strides=(2, 2), padding='same')(x)
    x = layers.add([x, residual])

    # Block 14
    x = layers.SeparableConv2D(1536, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)

    # Block 14 part 2
    x = layers.SeparableConv2D(2048, (3, 3), padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.Activation('relu')(x)

    # Fully Connected Layer
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(num_classes, activation='softmax')(x)

#     # Create model
    model = models.Model(inputs, x, name='xception')

    # Download and cache the Xception weights file
    # weights_path = get_file('xception_weights.h5', WEIGHTS_PATH, cache_subdir='models')

    # load weights
    # model.load_weights(weights_path)

    # compile model
    if num_classes == 2:
        model.compile(loss='binary_crossentropy', optimizer=optimizers.Adam(), metrics=['acc'])
    else:
        model.compile(loss='categorical_crossentropy', optimizer=optimizers.Adam(), metrics=['acc'])

    model.summary()

    if model_path:
        model.save(model_path)
        print('[Create model] Saved model to:', model_path)

    if model_json_path:
        model_json = model.to_json()
        with open(model_json_path, "w") as f:
            f.write(model_json)
        print('[Create model] Saved model JSON to:', model_json_path)

    return model

