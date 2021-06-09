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

from dltoolbox.training import train_segmenter


def train_model(model_config, config):
    """
    Trains the model according to its configuration
    Args:
        model_config: The model parameters
        config: The training settings

    Returns: None

    """

    if model_config["n_classes"] == 2:
        print("Detected a segmentation (2-classification) training")
        train_segmenter.train_model(model_config=model_config, config=config)
    else:
        raise AttributeError("Could not find training method for this model")
