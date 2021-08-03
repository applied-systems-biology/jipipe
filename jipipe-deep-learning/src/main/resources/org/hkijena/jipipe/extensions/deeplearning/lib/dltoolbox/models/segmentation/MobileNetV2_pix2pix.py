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
from dltoolbox.models import metrics
from dltoolbox import utils


def build_model(config):
    """
    Builds a costum unet with MobileNetV2 as encoder and pix2pix decoder model
    Args:
        config: The model parameters

    Returns: The model

    """

    img_shape = tuple(config["image_shape"])
    num_classes = config['n_classes']
    model_path = config['output_model_path']
    model_json_path = config["output_model_json_path"]

    # define the encoder with the MobileNetV2 model
    base_model = tf.keras.applications.MobileNetV2(input_shape=img_shape, include_top=False)

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
    down_stack = tf.keras.Model(inputs=base_model.input, outputs=base_model_outputs[-1])

    down_stack.trainable = False

    # define the decoder with the pix2pix generator model
    up_stack = [
        pix2pix.upsample(512, 3),  # 4x4 -> 8x8
        pix2pix.upsample(256, 3),  # 8x8 -> 16x16
        pix2pix.upsample(128, 3),  # 16x16 -> 32x32
        pix2pix.upsample(64, 3),  # 32x32 -> 64x64
    ]

    def unet_model(num_classes):
        inputs = tf.keras.layers.Input(shape=img_shape)

        # downsampling through the model
        skips = down_stack(inputs)
        x = skips[-1]
        # skips = reversed(skips[:-1])
        #skips = skips[::-1][1:]
        skips = base_model_outputs[::-1][1:]

        # upsampling and establishing the skip connections
        for up, skip in zip(up_stack, skips):

            print(x)
            x = up(x)
            concat = tf.keras.layers.Concatenate()
            x = concat([x, skip])

        # This is the last layer of the model
        last = tf.keras.layers.Conv2DTranspose(num_classes, 3, strides=2, padding='same')  # 64x64 -> 128x128

        x = last(x)

        return tf.keras.Model(inputs=inputs, outputs=x)

    model = unet_model(num_classes)
    model.compile(optimizer='adam',
                  loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
                  metrics=['accuracy'])

    # # Define the input and model
    # inputs = tf.keras.layers.Input(shape=img_shape)
    #
    # # Downsampling through the model
    # skips = encoder_block(img_shape=img_shape)
    # print(type(skips))
    # x = skips[-1]
    # skips = reversed(skips[:-1])
    #
    # print(skips)
    #
    # return
    #
    # base_model = tf.keras.applications.MobileNetV2(input_shape=img_shape, include_top=False)
    #
    # # Use the activations of these layers
    # layer_names = [
    #     'block_1_expand_relu',  # 64x64
    #     'block_3_expand_relu',  # 32x32
    #     'block_6_expand_relu',  # 16x16
    #     'block_13_expand_relu',  # 8x8
    #     'block_16_project',  # 4x4
    # ]
    # base_model_outputs = [base_model.get_layer(name).output for name in layer_names]
    #
    # # Create the feature extraction model
    # down_stack = tf.keras.Model(inputs=base_model.input, outputs=base_model_outputs[-1])
    #
    # down_stack.trainable = False
    #
    # up_stack = [
    #     pix2pix.upsample(512, 3),  # 4x4 -> 8x8
    #     pix2pix.upsample(256, 3),  # 8x8 -> 16x16
    #     pix2pix.upsample(128, 3),  # 16x16 -> 32x32
    #     pix2pix.upsample(64, 3),  # 32x32 -> 64x64
    # ]
    #
    # skips = down_stack(inputs)
    # x = skips[-1]
    #
    # skips = reversed(skips[:-1])
    # #skips = skips[:-1]#[::-1]
    #
    # print('x',x)
    #
    # #print(len(up_stack), len(skips))
    # print(len(up_stack))
    # print(len(skips))
    #
    # return
    #
    # # Upsampling and establishing the skip connections
    # # up_stack = decoder_block()
    #
    # for up, skip in zip(up_stack, skips):
    #     x = up(x)
    #     concat = tf.keras.layers.Concatenate()
    #     x = concat([x, skip])
    #
    # # This is the last layer of the model - depending of the number of output channels.
    # # The reason to output three channels is because there are three possible labels for each pixel.
    # # Think of this as multi-classification where each pixel being classified into three classes (if num_classes = 3).
    # last = tf.keras.layers.Conv2DTranspose(n_classes, 3, strides=2, padding='same')  # 64x64 -> 128x128
    #
    # x = last(x)
    #
    # # Create the final model
    # model = tf.keras.Model(inputs=inputs, outputs=x)
    #
    # # compile the model
    # model.compile(optimizer='adam',
    #               loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
    #               metrics=['accuracy', dice_coeff, IoU, matthews_corrcoef])
    #
    # model.summary()
    #
    # if model_path:
    #     model.save(model_path)
    #     print('[Create model] Saved model to:', model_path)
    #
    # if model_json_path:
    #     model_json = model.to_json()
    #     with open(model_json_path, "w") as f:
    #         f.write(model_json)
    #     print('[Create model] Saved model JSON to:', model_json_path)
    #
    # # save the model, model-architecture and model-config
    # utils.save_model_with_json(model=model, model_path=model_path, model_json_path=model_json_path, config=config)
    #
    # return model