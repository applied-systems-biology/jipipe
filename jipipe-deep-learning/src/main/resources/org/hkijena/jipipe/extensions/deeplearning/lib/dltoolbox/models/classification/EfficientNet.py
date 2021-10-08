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

import tensorflow as tf


def build_model(config):
    """
    Creates a EfficientNet model
    Args:
        config: the model parameters

    Returns: the model

    """

    img_shape = tuple(config["image_shape"])
    n_classes = config['n_classes']

    model = tf.keras.applications.efficientnet.EfficientNetB7(
        include_top=True,
        weights='imagenet',
        input_tensor=None,
        input_shape=img_shape,
        pooling=None,
        classes=n_classes,
        classifier_activation='softmax'
    )

    return model
