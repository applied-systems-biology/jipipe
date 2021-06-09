"""
Copyright by Jan-Philipp_Praetorius

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo
Figge
https://www.leibniz-hki.de/en/applied-systems-biology.html
HKI-Center for Systems Biology of Infection
Leibniz Institute for Natural Product Research and Infection Biology -
Hans Knöll Insitute (HKI)
Adolf-Reichwein-Straße 23, 07745 Jena, Germany
"""

import os

import keras

from dltoolbox.models.metrics import *


def load_and_compile_model(model_config, model_path, model=None) -> keras.Model:
    """
    Loads an compiles a model
    Args:
        model_config: Model configuration
        model_path: Path to the model file
        model: An existing model. If not None, model_path is ignored

    Returns: The model
    """

    from keras import models

    n_classes = model_config['n_classes']

    if model is None:
        print("[DLToolbox] Loading model from " + str(model_path))
        model = models.load_model(model_path, compile=False)

        # compile model, depend on the number of classes/segments (2 classes or more)
        if n_classes == 2:
            model.compile(optimizer='adam', loss=bce_dice_loss, metrics=[dice_loss])
        else:
            model.compile(optimizer='adam', loss=ce_dice_loss, metrics=[dice_loss])

    return model


def setup_devices(config=None):
    """
    Sets up GPU processing according to the current config
    Args:
        config: the config

    Returns: None

    """
    if config is None:
        config = {}
    os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"

    import tensorflow as tf

    cpu_config = config.get("cpus", "all")
    gpu_config = config.get("gpus", "all")

    # Configure CPUs
    if cpu_config == "all":
        print("Using all available CPUs")
    else:
        cpus = tf.config.list_physical_devices('CPU')
        visible_cpus = []
        for id in cpu_config:
            visible_cpus.append(cpus[id])

        tf.config.set_visible_devices(visible_cpus, device_type="CPU")

    # Configure GPUs
    if gpu_config == "all":
        print("Using all available GPUs")
    else:
        gpus = tf.config.list_physical_devices('GPU')
        visible_gpus = []
        for id in gpu_config:
            visible_gpus.append(gpus[id])

        tf.config.set_visible_devices(visible_gpus, device_type="GPU")

    # Enable/Disable device placement logging
    tf.debugging.set_log_device_placement(config.get("log-device-placement", False))


def sliding_window(img, stepSize, windowSize=(256, 256)):
    """
    Slide over the specified input image
    Args:
        img: the input image
        stepSize: the step size
        windowSize: the window size

    Returns:

    """
    # slide a window across the image
    for y in range(0, img.shape[0], stepSize):
        for x in range(0, img.shape[1], stepSize):
            # yield the current window, cause of reduce memory costs
            yield (x, y, img[y:y + windowSize[1], x:x + windowSize[0]])
