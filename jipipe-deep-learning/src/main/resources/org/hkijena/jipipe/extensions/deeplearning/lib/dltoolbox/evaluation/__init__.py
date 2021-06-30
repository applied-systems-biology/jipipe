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

from dltoolbox.evaluation import evaluate


def evaluate_data(config, model=None):
    """
    Evaluate the predicted data according to its labels
    Args:
        config: The evaluation settings
        model: The model. If None, it is loaded from the model config or config

    Returns: None

    """

    print("Perform evaluation method: " + config["evaluation_method"])

    if config['evaluation_method'] == "plot_probabilities":
        evaluate.plot_probabilities(config=config)
    elif config['evaluation_method'] == "treshold_predictions":
        evaluate.treshold_predictions(config=config)
    else:
        raise AttributeError("Unsupported evaluation method: " + config["evaluation_method"])
