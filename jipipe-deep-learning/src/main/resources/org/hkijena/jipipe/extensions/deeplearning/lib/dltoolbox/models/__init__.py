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
