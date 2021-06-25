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

    model_type = model_config['model_type']
    n_classes = model_config['n_classes']
    loss = model_config['loss']

    if model is None:
        print("[DLToolbox] Loading model from " + str(model_path))
        model = models.load_model(model_path, compile=False)

        if model_type == "segmentation":

            # default loss
            if loss == "":
                # compile model, depend on the number of classes/segments (2 classes or more)
                if n_classes == 2:
                    model.compile(optimizer='adam', loss=bce_dice_loss, metrics=[dice_loss])
                else:
                    model.compile(optimizer='adam', loss=ce_dice_loss, metrics=[dice_loss])
            else:
                # TODO: diese compilation hier abhängig von dem model machen
                pass

        elif model_type == "classification":

            # default loss
            if loss == "":
                # compile model, depend on the number of classes/segments (2 classes or more)
                if n_classes == 2:
                    model.compile(loss='binary_crossentropy', optimizer=keras.optimizers.Adam(), metrics=['acc'])
                else:
                    model.compile(loss='categorical_crossentropy', optimizer=keras.optimizers.Adam(), metrics=['acc'])
            else:
                # TODO: diese compilation hier abhängig von dem model machen
                pass

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


def sliding_window(img, step_size=(256, 256), window_size=(256, 256)):
    """
    Slide over the specified input image
    Args:
        img: the input image
        step_size: the step size (x1, x0)
        window_size: the window size (width, height)

    Returns: Generator of (x0, x1, window)
    """

    for x0 in range(0, img.shape[0], step_size[0]):
        for x1 in range(0, img.shape[1], step_size[1]):
            yield x0, x1, img[x0:x0 + window_size[1], x1:x1 + window_size[0]]


def preprocessing(img, mode):
    """
    Normalize the specified input image
    Args:
        img: the input image
        mode: the normalization mode

    Returns:
    """

    if mode == 'zero_one':
        return np.array(img) / 255.
    elif mode == 'minus_one_to_one':
        return np.array(img) / 127.5 - 1.
    raise AttributeError("Could not find valid normalization mode - {zero_one, minus_one_to_one}")
