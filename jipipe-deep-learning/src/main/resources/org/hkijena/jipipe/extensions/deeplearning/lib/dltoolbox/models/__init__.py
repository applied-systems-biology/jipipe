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

from dltoolbox.models.classification import VGG16
from dltoolbox.models.classification import Xception
from dltoolbox.models.classification import example_classifier
from dltoolbox.models.segmentation import SegNet
from dltoolbox.models.segmentation import FCN32
from dltoolbox.models.segmentation import MobileNetV2_pix2pix
from dltoolbox.models.GANs import pix2pix


def build_model(config):
    """
    Builds a model according to the configuration
    Args:
        config: Model parameters

    Returns: None

    """

    print("[DLToolbox] Building model of architecture " + config["architecture"])

    if config["architecture"] == "SegNet":
        return SegNet.build_model(config)
    elif config["architecture"] == "VGG16":
        return VGG16.build_model(config)
    elif config["architecture"] == "FCN32":
        return FCN32.build_model(config)
    elif config["architecture"] == "Xception":
        return Xception.build_model(config)
    elif config["architecture"] == "MobileNetV2_pix2pix":
        return MobileNetV2_pix2pix.build_model(config)
    elif config["architecture"] == "pix2pix":
        return pix2pix.build_model(config)
    elif config["architecture"] == "example_classifier":
        return example_classifier.build_model(config)
    else:
        raise AttributeError("Unsupported model-architecture: " + config["architecture"])
