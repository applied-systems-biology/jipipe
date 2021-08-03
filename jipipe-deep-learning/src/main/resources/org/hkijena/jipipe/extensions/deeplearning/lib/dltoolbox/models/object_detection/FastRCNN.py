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

from tensorflow.keras import Model
from models.vgg16_body import get_model_body
from tensorflow.keras.layers import Dense, Flatten, Dropout, Lambda
from fast_loss import fast_loss as loss
from models.roi_pooling_layer import RoipoolingLayer


class FastRCNN(Model):


    def __init__(self, num_classes, keep_prob=0.5):
        super(FastRCNN, self).__init__()
        self._num_classes = num_classes
        self._vgg16 = get_model_body()
        # roi pooling
        self._roi_pooling = RoipoolingLayer()
        self._flatten = Flatten()
        self._fc1 = Dense(4096, activation='tanh')
        self._dropout1 = Dropout(keep_prob)
        self._fc2 = Dense(4096, activation='tanh')
        self._dropout2 = Dropout(keep_prob)
        # predict k + 1 categories
        # (None, 128, 21)
        self._fc_cls = Dense(num_classes + 1, name='cls_output')
        # predict 4 * k
        # (None, 128, 80)
        self._fc_bbox = Dense(num_classes * 4, name='bbox_output')
        # 计算损失
        self._loss = Lambda(loss, name='fast_loss')

    def call(self, inputs, mask=None):

        image_data, labels, regions_target, bbox_targets, bbox_inside_weights, bbox_outside_weights = \
            inputs[0], inputs[1], inputs[2], inputs[3], inputs[4], inputs[5]
        # (None, 36, 36, 512)
        x = self._vgg16(image_data)
        # seletvie_search
        # (None, 128, 7, 7, 512)
        x = self._roi_pooling(x, regions_target)
        x = self._flatten(x)
        x = self._fc1(x)
        x = self._dropout1(x)
        x = self._fc2(x)
        x = self._dropout2(x)
        # (batch_size, 128, 21)
        cls_output = self._fc_cls(x)
        # (batch_size, 128, 80)
        bbox_output = self._fc_bbox(x)
        loss = self._loss([cls_output, labels, bbox_output, bbox_targets, bbox_inside_weights, bbox_outside_weights])
        return loss

from tensorflow.keras.layers import Layer
from roi.roi_tf import roi_pool_tf


class RoipoolingLayer(Layer):

    def __init__(self, **kwargs):
        super(RoipoolingLayer, self).__init__(**kwargs)

    def call(self, inputs, rois):
        output = roi_pool_tf(inputs, rois)
        return output


from tensorflow.keras.applications.vgg16 import VGG16
from tensorflow.keras import Model
from tensorflow.keras.layers import Input


def get_model_body(trainable=False):
    input_tensor = Input(shape=(567, 567, 3))
    vgg16_model = VGG16(input_tensor=input_tensor, include_top=False)
    if not trainable:
        for layer in vgg16_model.layers:
            # 让其不可训练
            layer.trainable = False
    # model = Model(inputs=vgg16_model.input, outputs=vgg16_model.get_layer('block5_conv3').output)
    model = Model(inputs=vgg16_model.input, outputs=vgg16_model.output)
    return model

#
# model = get_model_body()
# model.summary()