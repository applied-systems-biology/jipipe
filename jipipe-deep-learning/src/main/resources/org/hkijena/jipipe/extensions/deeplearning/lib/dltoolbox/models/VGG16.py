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

import os

os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"

from keras import layers
from keras import models
from keras import optimizers


#############################################################
#                           VGG16                           #
#############################################################
def build_model(config):
    """
    Creates a VGG16 model
    Args:
        config: the model parameters

    Returns: the model

    """

    img_shape = config['img_size']
    num_classes = config['n_classes']
    model_path = config['output_model_path']

    def secondConvBlock(input_tensor, num_filters):
        sec_conv = layers.Conv2D(num_filters, (3, 3), padding='same')(input_tensor)
        sec_conv = layers.Conv2D(num_filters, (3, 3), padding='same')(sec_conv)
        sec_conv = layers.BatchNormalization()(sec_conv)
        sec_conv = layers.Activation('relu')(sec_conv)
        sec_conv = layers.MaxPooling2D((2, 2))(sec_conv)
        return sec_conv

    def thirdConvBlock(input_tensor, num_filters):
        thi_conv = layers.Conv2D(num_filters, (3, 3), padding='same')(input_tensor)
        thi_conv = layers.Conv2D(num_filters, (3, 3), padding='same')(thi_conv)
        thi_conv = layers.Conv2D(num_filters, (3, 3), padding='same')(thi_conv)
        thi_conv = layers.BatchNormalization()(thi_conv)
        thi_conv = layers.Activation('relu')(thi_conv)
        thi_conv = layers.MaxPooling2D((2, 2))(thi_conv)
        return thi_conv

    inputs = layers.Input(img_shape)

    # 2nd - convolution - block
    sCB_0 = secondConvBlock(inputs, num_filters=64)
    sCB_1 = secondConvBlock(sCB_0, num_filters=128)

    # 3rd - convolution - block
    tCB_2 = thirdConvBlock(sCB_1, num_filters=256)
    tCB_3 = thirdConvBlock(tCB_2, num_filters=512)
    tCB_4 = thirdConvBlock(tCB_3, num_filters=512)

    # flatten the last convolutional layer
    flatten = layers.Flatten()(tCB_4)

    # dense-layers for classification
    dense_5 = layers.Dense(4096, activation='relu')(flatten)  # relu/leakyrelu
    dropout_6 = layers.Dropout(0.5)(dense_5)

    dense_7 = layers.Dense(4096, activation='relu')(dropout_6)  # relu/leakyrelu
    dropout_8 = layers.Dropout(0.5)(dense_7)

    # last layer for classification
    output = layers.Dense(num_classes, activation='softmax')(dropout_8)

    # create the model
    model = models.Model(inputs=[inputs], outputs=[output])

    # compile model
    model.compile(loss='categorical_crossentropy', optimizer=optimizers.Adam(), metrics=['acc'])

    model.summary()

    if model_path:
        model.save(model_path)
        print('save model to:', model_path)

    return model
