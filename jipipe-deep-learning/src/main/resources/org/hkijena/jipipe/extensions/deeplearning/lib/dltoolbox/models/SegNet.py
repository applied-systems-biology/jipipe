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

import os


from keras import layers, models

# import all metrics for model-compilation
from dltoolbox.models.metrics import *


def build_model(config):
    """
    Builds a SegNet model
    Args:
        config: The model parameters

    Returns: The model

    """

    img_shape = (config['img_size'], config['img_size'], 3)
    reg_method = config['regularization_method']
    reg_method_rate = config['regularization_lambda']
    nClasses = config['n_classes']
    model_path = config['output_model_path']
    model_json_path = config["output_model_json_path"]

    def conv_block(input_tensor, num_filters, kernel_size=7, reg_method='GaussianDropout'):

        encoder = layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(input_tensor)
        encoder = layers.BatchNormalization()(encoder)
        encoder = layers.Activation('relu')(encoder)
        encoder = layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(encoder)
        encoder = layers.BatchNormalization()(encoder)

        if reg_method == 'Dropout':
            encoder = layers.Dropout(reg_method_rate)(encoder)
        elif reg_method == 'GaussianDropout':
            encoder = layers.GaussianDropout(reg_method_rate)(encoder)
        elif reg_method == 'GaussianNoise':
            encoder = layers.GaussianNoise(reg_method_rate)(encoder)

        encoder = layers.Activation('relu')(encoder)

        return encoder

    def encoder_block(input_tensor, num_filters):
        encoder = conv_block(input_tensor, num_filters)
        encoder_pool = layers.MaxPooling2D((2, 2), strides=(2, 2))(encoder)

        return encoder_pool, encoder

    def decoder_block(input_tensor, concat_tensor, num_filters, kernel_size=7):

        decoder = layers.Conv2DTranspose(num_filters, (2, 2), strides=(2, 2), padding='same')(input_tensor)
        decoder = layers.concatenate([concat_tensor, decoder], axis=-1)
        decoder = layers.BatchNormalization()(decoder)
        decoder = layers.Activation('relu')(decoder)
        decoder = layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(decoder)
        decoder = layers.BatchNormalization()(decoder)
        decoder = layers.Activation('relu')(decoder)
        decoder = layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(decoder)
        decoder = layers.BatchNormalization()(decoder)

        if reg_method == 'Dropout':
            decoder = layers.Dropout(reg_method_rate)(decoder)
        elif reg_method == 'GaussianDropout':
            decoder = layers.GaussianDropout(reg_method_rate)(decoder)
        elif reg_method == 'GaussianNoise':
            decoder = layers.GaussianNoise(reg_method_rate)(decoder)

        decoder = layers.Activation('relu')(decoder)

        return decoder

    inputs = layers.Input(shape=img_shape)

    # perform regularization on input / visible - layer
    if reg_method == 'Dropout':
        inputs_reg = layers.Dropout(reg_method_rate)(inputs)
    elif reg_method == 'GaussianDropout':
        inputs_reg = layers.GaussianDropout(reg_method_rate)(inputs)
    elif reg_method == 'GaussianNoise':
        inputs_reg = layers.GaussianNoise(reg_method_rate)(inputs)

    # distinguish for any or none regularization method
    if reg_method == 'none':
        encoder0_pool, encoder0 = encoder_block(inputs, 16)
    else:
        encoder0_pool, encoder0 = encoder_block(inputs_reg, 16)

    encoder1_pool, encoder1 = encoder_block(encoder0_pool, 32)
    encoder2_pool, encoder2 = encoder_block(encoder1_pool, 64)
    encoder3_pool, encoder3 = encoder_block(encoder2_pool, 128)
    encoder4_pool, encoder4 = encoder_block(encoder3_pool, 256)
    center = conv_block(encoder4_pool, 512)
    decoder4 = decoder_block(center, encoder4, 256)
    decoder3 = decoder_block(decoder4, encoder3, 128)
    decoder2 = decoder_block(decoder3, encoder2, 64)
    decoder1 = decoder_block(decoder2, encoder1, 32)
    decoder0 = decoder_block(decoder1, encoder0, 16)

    # if labels only have 2 dimension, remove the last one, e.g.: (256,256,1) => (256,256)
    # CAUTION: format (256,256) is not compatible with keras.ImageDataGenerator
    # def mysqueeze(out):
    #     return tf.keras.backend.squeeze(out, axis=-1)

    if nClasses == 2:
        outputs = layers.Conv2D(1, (1, 1), activation='sigmoid')(decoder0)
        # if labels have only 2 dimension, e.g.: (256,256)
        # outputs = layers.Lambda(mysqueeze)(outputs)
    else:
        outputs = layers.Conv2D(nClasses, (1, 1), activation='sigmoid')(decoder0)

    # create the model
    model = models.Model(inputs=[inputs], outputs=[outputs])

    # compile model, depend on the number of classes/segments (2 classes or more)
    if nClasses == 2:
        model.compile(optimizer='adam', loss=bce_dice_loss, metrics=[dice_loss])
    else:
        model.compile(optimizer='adam', loss=ce_dice_loss, metrics=[dice_loss])

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
