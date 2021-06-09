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

from dltoolbox.models import SegNet
from dltoolbox.models import VGG16


def build_model(config):
    """
    Builds a model according to the configuration
    Args:
        config: Model parameters

    Returns: None

    """

    print("Building model of architecture " + config["architecture"])

    if config["architecture"] == "SegNet":
        SegNet.build_model(config)
    elif config["architecture"] == "VGG16":
        VGG16.build_model(config)
