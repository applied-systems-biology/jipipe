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

Script to create a mobilenet model
"""

import tensorflow as tf


def build_model(config, **kwargs):
    """
    Build MobileNetV2 classification model.
    Set weights='imagenet' to use a pretrained model.

    Args:
        config: The model parameters

    Returns: The model
    """

    img_shape = tuple(config["image_shape"])
    num_classes = config['n_classes']

    model = tf.keras.applications.mobilenet_v2.MobileNetV2(
        input_shape=img_shape,
        alpha=1.0,
        include_top=True,
        input_tensor=None,
        pooling=None,
        classes=num_classes,
        classifier_activation='softmax',
        **kwargs
    )

    return model
