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

from dltoolbox.models.classification import VGG16
from dltoolbox.models.classification import Xception
from dltoolbox.models.classification import EfficientNet
from dltoolbox.models.classification import example_classifier
from dltoolbox.models.segmentation import SegNet
from dltoolbox.models.segmentation import FCN32
from dltoolbox.models.segmentation import MobileNetV2_pix2pix
from dltoolbox.models.GANs import pix2pix

from dltoolbox.models import metrics
from dltoolbox import utils


def build_model(config):
    """
    Builds a model according to the configuration
    Args:
        config: Model parameters

    Returns: The model

    """

    print("[DLToolbox] Building model of architecture " + config["architecture"])

    if config["architecture"] == "SegNet":
        model = SegNet.build_model(config)
    elif config["architecture"] == "VGG16":
        model = VGG16.build_model(config)
    elif config["architecture"] == "FCN32":
        model = FCN32.build_model(config)
    elif config["architecture"] == "Xception":
        model = Xception.build_model(config)
    elif config["architecture"] == "EfficientNet":
        model = EfficientNet.build_model(config)
    elif config["architecture"] == "MobileNetV2_pix2pix":
        model = MobileNetV2_pix2pix.build_model(config)
    elif config["architecture"] == "pix2pix":
        model = pix2pix.build_model(config)
    elif config["architecture"] == "example_classifier":
        model = example_classifier.build_model(config)
    else:
        raise AttributeError("Unsupported model-architecture: " + config["architecture"])

    # get parameter
    num_classes = config['n_classes']
    model_type = config['model_type']
    learning_rate = config['learning_rate']
    model_path = config['output_model_path']
    model_json_path = config['output_model_json_path']
    weight_regularization = config['weight_regularization']
    weight_regularization_lambda = config['weight_regularization_lambda']

    # if specified: add weight-regularization
    if weight_regularization == "l1":
        l1_reg = tf.keras.regularizers.l1(weight_regularization_lambda)

        for layer in model.layers:
            if isinstance(layer, tf.keras.layers.Conv2D) or \
                    isinstance(layer, tf.keras.layers.SeparableConv2D) or \
                    isinstance(layer, tf.keras.layers.Conv2DTranspose) or \
                    isinstance(layer, tf.keras.layers.DepthwiseConv2D) or \
                    isinstance(layer, tf.keras.layers.Dense):

                layer.kernel_regularizer = l1_reg

        print("[DLToolbox] Apply weight regularization: L1")
    elif weight_regularization == "l2":
        l2_reg = tf.keras.regularizers.l2(weight_regularization_lambda)

        for layer in model.layers:
            if isinstance(layer, tf.keras.layers.Conv2D) or \
                    isinstance(layer, tf.keras.layers.SeparableConv2D) or \
                    isinstance(layer, tf.keras.layers.Conv2DTranspose) or \
                    isinstance(layer, tf.keras.layers.DepthwiseConv2D) or \
                    isinstance(layer, tf.keras.layers.Dense):

                layer.kernel_regularizer = l2_reg

        print("[DLToolbox] Apply weight regularization: L2")
    else:
        print("[DLToolbox] Do not apply any weight regularization (L1, L2)")

    # get metrics corresponding to the model
    model_metrics = metrics.get_metrics(model_type, num_classes)

    # compile model, depend on the number of classes/segments (2 classes or more) and model-type
    adam = tf.keras.optimizers.Adam(lr=learning_rate)

    if model_type == "segmentation":

        if num_classes == 2:
            model.compile(optimizer=adam, loss=metrics.bce_dice_loss, metrics=model_metrics)
        else:
            model.compile(optimizer=adam, loss=metrics.ce_dice_loss, metrics=model_metrics)

    elif model_type == "classification":

        if num_classes == 2:
            model.compile(loss='binary_crossentropy', optimizer=adam, metrics=model_metrics)
        else:
            model.compile(loss='categorical_crossentropy', optimizer=adam, metrics=model_metrics)

    else:
        raise AttributeError("Unsupported model_type: " + model_type)

    model.summary()

    # save the model, model-architecture and model-config
    utils.save_model_with_json(model=model,
                               model_path=model_path,
                               model_json_path=model_json_path,
                               model_config=config,
                               operation_config=None)

    return model
