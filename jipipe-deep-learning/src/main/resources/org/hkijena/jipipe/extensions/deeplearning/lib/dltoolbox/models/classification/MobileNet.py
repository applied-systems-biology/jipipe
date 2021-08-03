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
from dltoolbox import utils
from dltoolbox.models import metrics


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
    model_path = config['output_model_path']
    model_json_path = config["output_model_json_path"]
    model_type = config['model_type']
    learning_rate = config['learning_rate']

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
    utils.save_model_with_json(model=model, model_path=model_path, model_json_path=model_json_path, config=config)

    return model
