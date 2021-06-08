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
