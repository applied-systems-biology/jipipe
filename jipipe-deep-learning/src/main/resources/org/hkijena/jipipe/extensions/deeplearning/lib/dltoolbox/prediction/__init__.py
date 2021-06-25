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

from dltoolbox.prediction import predict_samples
from dltoolbox.prediction import predict_cross_validation


def predict_data(model_config, config, model=None):
    """
    Predicts data according to its configuration
    Args:
        model_config: The model parameters
        config: The training settings

    Returns: None

    """

    if config['prediction_type'] == "standard":
        print("[Predict] standard prediction procedure with specified samples")
        predict_samples.predict_samples(model_config=model_config, config=config, model=model)
    elif config['prediction_type'] == "cross-validation":
        print("[Predict] cross-validation prediction procedure with information table")
        predict_cross_validation.predict_cross_validation(model_config=model_config, config=config, model=model)
    else:
        raise AttributeError("Could not find valid prediction_type for prediction - {standard, cross-validation}")