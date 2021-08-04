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

Script to predict a network
"""

from dltoolbox.prediction import predict_unet
from dltoolbox.prediction import predict_classifier


def predict_data(model_config, config, model=None):
    """
    Predicts data according to its configuration
    Args:
        model_config: The model parameters
        config: The training settings

    Returns: None

    """

    if model_config['model_type'] == "segmentation":
        print("[Predict] Prediction procedure with specified samples via unet")
        predict_unet.predict_samples(model_config=model_config, config=config, model=model)
    elif model_config['model_type'] == "classification":
        print("[Predict] Prediction procedure with specified samples via classifier")
        predict_classifier.predict_samples(model_config=model_config, config=config, model=model)
    else:
        raise AttributeError("Could not find a valid model type:" + str(model_config['model_type']))
