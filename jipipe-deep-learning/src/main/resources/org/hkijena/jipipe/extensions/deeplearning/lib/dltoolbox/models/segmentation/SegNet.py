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
from dltoolbox.models import metrics
from dltoolbox import utils


def build_model(config):
    """
    Builds a SegNet model
    Args:
        config: The model parameters

    Returns: The model

    """

    img_shape = tuple(config["image_shape"])
    reg_method = config['regularization_method']
    reg_method_rate = config['regularization_lambda']
    num_classes = config['n_classes']
    learning_rate = config['learning_rate']
    model_path = config['output_model_path']
    model_json_path = config["output_model_json_path"]
    model_type = config['model_type']

    def conv_block(input_tensor, num_filters, kernel_size=3, reg_method='GaussianDropout'):
        """

        Args:
            input_tensor:
            num_filters:
            kernel_size: kernel-size. CAUTION: original size = 3
            reg_method:

        Returns:

        """

        encoder = tf.keras.layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(input_tensor)
        encoder = tf.keras.layers.BatchNormalization()(encoder)
        encoder = tf.keras.layers.Activation('relu')(encoder)
        encoder = tf.keras.layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(encoder)
        encoder = tf.keras.layers.BatchNormalization()(encoder)

        if reg_method == 'Dropout':
            encoder = tf.keras.layers.Dropout(reg_method_rate)(encoder)
        elif reg_method == 'GaussianDropout':
            encoder = tf.keras.layers.GaussianDropout(reg_method_rate)(encoder)
        elif reg_method == 'GaussianNoise':
            encoder = tf.keras.layers.GaussianNoise(reg_method_rate)(encoder)

        encoder = tf.keras.layers.Activation('relu')(encoder)

        return encoder

    def encoder_block(input_tensor, num_filters):
        encoder = conv_block(input_tensor, num_filters)
        encoder_pool = tf.keras.layers.MaxPooling2D((2, 2), strides=(2, 2))(encoder)

        return encoder_pool, encoder

    def decoder_block(input_tensor, concat_tensor, num_filters, kernel_size=3):
        """

        Args:
            input_tensor:
            concat_tensor:
            num_filters:
            kernel_size: kernel-size. CAUTION: original size = 3

        Returns:

        """

        decoder = tf.keras.layers.Conv2DTranspose(num_filters, (2, 2), strides=(2, 2), padding='same')(input_tensor)
        decoder = tf.keras.layers.concatenate([concat_tensor, decoder], axis=-1)
        decoder = tf.keras.layers.BatchNormalization()(decoder)
        decoder = tf.keras.layers.Activation('relu')(decoder)
        decoder = tf.keras.layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(decoder)
        decoder = tf.keras.layers.BatchNormalization()(decoder)
        decoder = tf.keras.layers.Activation('relu')(decoder)
        decoder = tf.keras.layers.Conv2D(num_filters, (kernel_size, kernel_size), padding='same')(decoder)
        decoder = tf.keras.layers.BatchNormalization()(decoder)

        if reg_method == 'Dropout':
            decoder = tf.keras.layers.Dropout(reg_method_rate)(decoder)
        elif reg_method == 'GaussianDropout':
            decoder = tf.keras.layers.GaussianDropout(reg_method_rate)(decoder)
        elif reg_method == 'GaussianNoise':
            decoder = tf.keras.layers.GaussianNoise(reg_method_rate)(decoder)

        decoder = tf.keras.layers.Activation('relu')(decoder)

        return decoder

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
        encoder0_pool, encoder0 = encoder_block(inputs_reg, 16)
    else:
        encoder0_pool, encoder0 = encoder_block(inputs, 16)

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
   
    if num_classes == 2:
        outputs = tf.keras.layers.Conv2D(1, (1, 1), activation='sigmoid')(decoder0)
    else:
        outputs = tf.keras.layers.Conv2D(num_classes, (1, 1), activation='sigmoid')(decoder0)

    # create the model
    model = tf.keras.models.Model(inputs=[inputs], outputs=[outputs])

    # get all metrics
    model_metrics = metrics.get_metrics(model_type, num_classes)

    # compile model, depend on the number of classes/segments (2 classes or more)
    adam = tf.keras.optimizers.Adam(lr=learning_rate)
    if num_classes == 2:
        model.compile(optimizer=adam, loss=metrics.bce_dice_loss, metrics=model_metrics)
    else:
        model.compile(optimizer=adam, loss=metrics.ce_dice_loss, metrics=model_metrics)

    model.summary()

    # save the model, model-architecture and model-config
    utils.save_model_with_json(model=model,
                               model_path=model_path,
                               model_json_path=model_json_path,
                               model_config=config,
                               operation_config=None)

    return model
