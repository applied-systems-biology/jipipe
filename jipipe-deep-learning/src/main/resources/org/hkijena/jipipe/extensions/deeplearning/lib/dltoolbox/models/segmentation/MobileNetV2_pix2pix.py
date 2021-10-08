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
"""

import tensorflow as tf

from dltoolbox.models.GANs import pix2pix


def build_model(config):
    """
    Builds a costum unet with MobileNetV2 as encoder and pix2pix decoder model
    Args:
        config: The model parameters

    Returns: The model

    """

    img_shape = tuple(config["image_shape"])
    n_classes = config['n_classes']

    # MobileNetV2 needs to have 3 input channels: change the channel size, if necessary
    if img_shape[-1] != 3:
        img_shape = (img_shape[0], img_shape[1], 3)
        print(f'[Build model] Changed channel size to 3 - new model input shape: {img_shape}')

    # define the encoder with the MobileNetV2 model
    base_model = tf.keras.applications.MobileNetV2(input_shape=img_shape, include_top=False, weights='imagenet')

    # use the activations of these layers
    layer_names = [
        'block_1_expand_relu',  # 64x64
        'block_3_expand_relu',  # 32x32
        'block_6_expand_relu',  # 16x16
        'block_13_expand_relu',  # 8x8
        'block_16_project',  # 4x4
    ]
    base_model_outputs = [base_model.get_layer(name).output for name in layer_names]

    # create the feature extraction model
    # down_stack = tf.keras.Model(inputs=base_model.input, outputs=base_model_outputs[-1])
    down_stack = tf.keras.Model(inputs=base_model.input, outputs=base_model_outputs)

    down_stack.trainable = False

    # define the decoder with the pix2pix generator model
    up_stack = [
        pix2pix.upsample(512, 3),  # 4x4 -> 8x8
        pix2pix.upsample(256, 3),  # 8x8 -> 16x16
        pix2pix.upsample(128, 3),  # 16x16 -> 32x32
        pix2pix.upsample(64, 3),  # 32x32 -> 64x64
    ]

    def unet_model(output_channels: int):

        inputs = tf.keras.layers.Input(shape=img_shape)

        # downsampling through the model
        skips = down_stack(inputs)
        x = skips[-1]
        skips = reversed(skips[:-1])

        # upsampling and establishing the skip connections
        for up, skip in zip(up_stack, skips):
            x = up(x)
            concat = tf.keras.layers.Concatenate()
            x = concat([x, skip])

        # This is the last layer of the model
        last = tf.keras.layers.Conv2DTranspose(
            filters=output_channels,
            kernel_size=3,
            strides=2,
            padding='same')

        x = last(x)

        return tf.keras.Model(inputs=inputs, outputs=x)

    # define the number of output channels: binary (2) -> 1 , else -> n_classes
    if n_classes == 2:
        num_output_channels = 1
    else:
        num_output_channels = n_classes

    # create the model
    model = unet_model(output_channels=num_output_channels)

    return model