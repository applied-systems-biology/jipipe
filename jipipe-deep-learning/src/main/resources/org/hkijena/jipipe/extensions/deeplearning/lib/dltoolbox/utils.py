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
from glob import glob

import numpy as np
from skimage import img_as_float32, img_as_ubyte
from skimage import filters
from skimage import io
import keras
import cv2
from scipy.ndimage.interpolation import map_coordinates
from scipy.ndimage.filters import gaussian_filter
import tensorflow as tf

from dltoolbox.models.metrics import *


def imread_collection(pattern, load_func=io.imread, **kwargs):
    """
    Custom version of skimage imread_collection that correctly handles TIFF files
    Args:
        load_func: function used for loading an image. the first argument will be the image path (default: skimage.io.imread)
        pattern: glob pattern

    Returns: list of loaded images

    """
    files = glob(pattern)
    files.sort()
    imgs = []
    for file in files:
        print("[I/O] Reading", file, "...")
        imgs.append(load_func(file, **kwargs))
    return imgs


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

        else:
            raise AttributeError("Unsupported model_type: " + model_type)

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
        try:
            for id in gpu_config:
                visible_gpus.append(gpus[id])
        except:
            print('[setup_devices] please specify <all> or set the desired gpus in a list (e.g. [0,1])')

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

    print("[utils.preprocessing] preprocess image with mode: <{0}>".format(mode))

    if mode == 'zero_one':
        return img_as_float32(img / 255.)
    elif mode == 'minus_one_to_one':
        return img_as_float32(img / (127.5 - 1.))
    else:
        raise AttributeError("Could not find valid normalization mode - {zero_one, minus_one_to_one}")

    # denominator = tf.constant(img, dtype=tf.float32)
    # if mode == 'zero_one':
    #     divisor = tf.constant(255., dtype=tf.float32)
    # elif mode == 'minus_one_to_one':
    #     divisor = tf.constant(127.5 - 1., dtype=tf.float32)
    # else:
    #     raise AttributeError("Could not find valid normalization mode - {zero_one, minus_one_to_one}")
    #
    # result = tf.constant(tf.truediv(denominator, divisor), dtype=tf.float32) #.numpy()
    #
    # return result


def binarizeAnnotation(img, use_otsu, convertToGray=False):
    """
    Binarize input image
    Args:
        img: input image is an RGB/grayscaled-image with the origin color where annotated signal occurs
        use_otsu: use the otsu method for trehsolding
        convertToGray: convert the image to grayscale image before thresholding

    Returns: output image is a grayscaled binary image with values 0 (foreground) and 255 (background)

    """

    # convert to gray-scaled image if chosen
    if convertToGray:
        img = cv2.cvtColor(img, cv2.COLOR_RGB2GRAY)

    # if there are only one unique value, return 0 (background image)
    if len(np.unique(img)) == 1:
        return np.zeros(shape=img.shape[:2], dtype=np.uint8)

    # perform either a otsu-thresholding or a fixed binary thresholding
    if use_otsu:
        thresh = filters.threshold_otsu(img)
        binary = img < thresh
    else:
        binary = img < 255

    return img_as_ubyte(binary)


def elastic_transform(image, alpha=None, sigma=None, seed=None):
    """
    Elastic deformation of images as described in [Simard2003] Simard, Steinkraus and Platt,
    "Best Practices for Convolutional Neural Networks applied to Visual Document Analysis", in Proc. of the
    International Conference on Document Analysis and Recognition, 2003.

    Args:
        image: input image (grayscaled or 3-channel 2D-image)
        alpha: lambda operator to enhance the transformation
        sigma: variance for Gaussian filtering
        seed: random seed

    Returns:
        transformed image, binary if input image also was binary

    """

    # check if input image is an binary image to also provide a binary image as an output image
    is_binary = True if len(np.unique(image)) == 2 else False

    if alpha == None:
        alpha = image.shape[0] + image.shape[1]

    if sigma == None:
        sigma = (image.shape[0] + image.shape[1]) * 0.03

    if seed is None:
        random_state = np.random.RandomState(None)
    else:
        random_state = np.random.RandomState(seed)

    # apply filter only on x an y dimension
    shape = image.shape[:2]

    dx = gaussian_filter((random_state.rand(*shape) * 2 - 1), sigma, mode="constant", cval=0) * alpha
    dy = gaussian_filter((random_state.rand(*shape) * 2 - 1), sigma, mode="constant", cval=0) * alpha

    x, y = np.meshgrid(np.arange(shape[0]), np.arange(shape[1]), indexing='ij')

    indices = np.reshape(x + dx, (-1, 1)), np.reshape(y + dy, (-1, 1))

    output = np.zeros_like(image)
    if len(image.shape) == 2:
        output = np.array(map_coordinates(image, indices, order=1).reshape(shape))
    else:
        output[:, :, 0] = np.array(map_coordinates(image[:, :, 0], indices, order=1).reshape(shape))
        output[:, :, 1] = np.array(map_coordinates(image[:, :, 1], indices, order=1).reshape(shape))
        output[:, :, 2] = np.array(map_coordinates(image[:, :, 2], indices, order=1).reshape(shape))

    # if necessary threshold to provide a binary output image
    if is_binary:
        output = img_as_ubyte((output > 0) * 255)

    return output


def check_for_tif_files(image_dir):
    """
    Check if images in a specified directory are tif(f) images or not
    Args:
        image_dir: directory path where the images are located

    Returns: True that imags are tif-files - False if not

    """

    image_paths = glob(image_dir)

    # extract only filenames
    image_paths = [os.path.basename(file) for file in image_paths]

    is_tifffile = []

    # check for ending
    for file in image_paths:
        if file.endswith('.tif') or file.endswith('.tiff'):
            is_tifffile.append(True)
        else:
            is_tifffile.append(False)

    # distinguish for tif-file(s)
    if np.count_nonzero(is_tifffile) > 0:
        return True
    else:
        return False


def validate_image_shape(model_shape, images):
    """
    Validate between model and input shape. Expand dimensions in loaded image if necessary

    Args:
        model_shape: should shape of the model (input or output)
        images: images load from directory

    Returns: image array in the required shape

    """

    # Read meta data (works only on tif files)
    # from PIL import Image
    # from PIL.TiffTags import TAGS
    #
    # with Image.open(input_dir_0) as img:
    #     #print(img.tag)
    #     meta_dict = {TAGS[key]: img.tag[key] for key in img.tag.keys()}
    # print(meta_dict)
    # print(meta_dict['ImageWidth'])

    # Apply own function to imread-collection
    # def imread_convert(f):
    #     return imread(f).astype(np.uint8)
    #
    # ic = ImageCollection('/tmp/*.png', load_func=imread_convert)

    images_unique_shapes = [img.shape for img in images]
    images_unique_shapes = list(set(images_unique_shapes))

    if len(images_unique_shapes) != 1:
        print('[validate_image_shape] CAUTION: shape of images have not all the same shape:', images_unique_shapes)

    image_shape = images_unique_shapes[0]

    if image_shape != model_shape[1:]:
        print(f'[validate_image_shape] CAUTION: image shape <{image_shape}> != from model shape <{model_shape[1:]}>')

    images_arr = np.array(images)

    # Image and model shape match (including batch-dimension)
    if len(images_arr.shape) == len(model_shape):
        return images_arr

    # Transfer to numpy required array with an additional channel dimension: (height,width) -> (height,width,1)
    if len(images_arr.shape) == 3:
        images_arr = np.expand_dims(images_arr, axis=-1)

        if images_arr.shape[1:] != model_shape[1:]:
            print(
                f'[validate_image_shape] WARNING: image shape <{images_arr.shape[1:]}> != from model shape <{model_shape[1:]}>')

    return images_arr
