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
from pathlib import Path
import json
import numpy as np
import tensorflow as tf
import cv2
from matplotlib import pyplot as plt
from scipy.ndimage.filters import gaussian_filter
from scipy.ndimage.interpolation import map_coordinates
from skimage import filters
from skimage import img_as_float32, img_as_ubyte
from skimage import io

from dltoolbox.models import metrics


def imread_collection(pattern, load_func=io.imread, verbose=False, **kwargs):
    """
    Custom version of skimage imread_collection that correctly handles TIFF files
    Args:
        verbose: verbose during the image file iteration
        load_func: function used for loading an image. the first argument will be the image path (default: skimage.io.imread)
        pattern: glob pattern

    Returns: list of loaded images

    """
    files = glob(pattern)
    files.sort()
    imgs = []
    for file in files:
        if verbose:
            print("[I/O] Reading", file, "...")
        imgs.append(load_func(file, **kwargs))
    return imgs


def load_and_compile_model(model_config, model_path, model=None) -> tf.keras.Model:
    """
    Loads an compiles a model
    Args:
        model_config: Model configuration
        model_path: Path to the model file
        model: An existing model. If not None, model_path is ignored

    Returns: The model
    """

    model_type = model_config['model_type']
    num_classes = model_config['n_classes']
    learning_rate = model_config['learning_rate']

    # create the adam optimizer
    adam = tf.keras.optimizers.Adam(lr=learning_rate)

    if model is None:
        print("[DLToolbox] Loading model from " + str(model_path))
        model = tf.keras.models.load_model(model_path, compile=False)

        # get all metrics
        model_metrics = metrics.get_metrics(model_type, num_classes)

        if model_type == "segmentation":

            # compile model, depend on the number of classes/segments (2 classes or more)
            if num_classes == 2:
                model.compile(optimizer=adam, loss=metrics.bce_dice_loss, metrics=model_metrics)
            else:
                model.compile(optimizer=adam, loss=metrics.ce_dice_loss, metrics=model_metrics)

        elif model_type == "classification":

            # compile model, depend on the number of classes/segments (2 classes or more)
            if num_classes == 2:
                model.compile(loss='binary_crossentropy', optimizer=adam, metrics=model_metrics)
            else:
                model.compile(loss='categorical_crossentropy', optimizer=adam, metrics=model_metrics)

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

    Returns: image array in the required shape. Take care of situation if only 1 images is loaded.

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


def plot_window(img, img_binary, title=None):
    """
    Plot the cut-out original image with drawn contours based on the binary annotations.

    Args:
        img ([type]): original image
        img_binary ([type]): binary image
        title: general title for the figure
    """

    img_binary = np.squeeze(img_binary)

    fig, ax_arr = plt.subplots(1, 2, figsize=(18, 8))
    ax1, ax2 = ax_arr.ravel()

    if len(img.shape) == 3:
        ax1.imshow(img)
    else:
        ax1.imshow(img, cmap='gray')
    ax1.set_title("Original image with type: {}".format(img.dtype), fontsize=18)
    ax1.contour(img_binary, colors='green', linewidths=2)

    ax2.imshow(img_binary, cmap='gray')
    ax2.set_title("Annotations image with type: {}".format(img_binary.dtype), fontsize=20)

    if title is not None:
        fig.suptitle('{}'.format(title), fontsize=30)

    plt.tight_layout()
    plt.show()


def save_model_with_json(model, model_path, model_json_path, config):
    """
    Save the model, its architecture as a JSON and the config file.
    Args:
        model: the corresponding model
        model_path: the model path
        model_json_path: the path of the model architecture within a JSON
        config: the config file to create the model

    Returns:

    """

    # create the model directory
    save_directory = str(Path(model_path).parent)
    if not os.path.exists(save_directory):
        os.makedirs(save_directory)
        print('[Save model] create directory folder for model:', save_directory)

    if model_path:
        model.save(model_path)
        print('[Save model] Saved model to:', model_path)

    if model_json_path:
        model_json = model.to_json()
        with open(model_json_path, "w") as f:
            f.write(model_json)
        print('[Save model] Saved model JSON to:', model_json_path)

        config_save_path = Path(model_json_path).parent / 'model-config.json'
        with open(config_save_path, "w+") as f:
            json.dump(config, f)
        print('[Save model] Save model config file JSON to:', config_save_path)
