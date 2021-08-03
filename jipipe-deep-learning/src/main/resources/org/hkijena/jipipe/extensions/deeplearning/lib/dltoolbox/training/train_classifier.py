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

Script to train a segmentation network
"""

import os
import numpy as np
import pandas as pd
from glob import glob
import math
from sklearn.model_selection import train_test_split
from skimage import io
from pathlib import Path
import tensorflow as tf
from dltoolbox import utils
from dltoolbox.evaluation import evaluate
from dltoolbox.training import callbacks


def train_model(model_config, config, model=None):
    """
    Trains an existing model. The existing model path is either extracted from a parameter input_model_path of the config,
    or if it does not exist, from the output_model_path of the model config
    Args:
        model: The model. If None, it is loaded from the model config or config
        model_config: Parameters of the model
        config: Training parameters

    Returns: The trained model
    """

    # assign hyper-parameter for training procedure
    input_dir = config['input_dir']
    label_dir = config['label_dir']
    n_epochs = config['max_epochs']
    batch_size = config['batch_size']
    validation_split = config['validation_split']
    input_model_path = config["input_model_path"] if "input_model_path" in config else model_config['output_model_path']
    output_model_path = config['output_model_path']
    output_model_json_path = config['output_model_json_path']
    augment_factor = config['augmentation_factor']
    log_dir = config['log_dir']

    # load the model
    model = utils.load_and_compile_model(model_config, input_model_path, model)

    # check whether the input data are specified within a table OR as images
    input_as_images = not str(input_dir).endswith('csv')

    print('[Train model] Input is represented as images:', input_as_images)

    if input_as_images:

        # read all filenames from input files and the corresponding labels table
        X_filenames = np.sort(glob(input_dir))
        Y_df = pd.read_csv(label_dir, index_col=0)

        X_paths = []
        Y = []

        # match input images with the labels table
        for label_filename, label_value in Y_df.iterrows():

            # skip objects where no label exist
            if math.isnan(label_value['label']):
                continue

            for input_filename in X_filenames:

                match = str(label_value['filename']) in input_filename
                # if the filename of the image contains the label-filename the match condition is true
                if match:
                    X_paths.append(input_filename)
                    Y.append(label_value['label'])

                    # print(f"label_filename: {label_filename} - input_filename: {input_filename} - match: {match}")
                    break

    else:

        df_data = pd.read_csv(label_dir, index_col=0)

        X_paths = df_data['filename'].tolist()
        Y = df_data['label'].tolist()

    # read the matching input images and label values
    X = io.imread_collection(X_paths)

    print('[Train model] Input-images:', len(X), ', Label-values:', len(Y))

    assert len(X) == len(Y) > 0

    # validate input data
    x = utils.validate_image_shape(model.input_shape, images=X)

    print('[Train model] Input data:\t', x.shape)

    assert not np.any(np.isnan(x)), "[WARNING] Input data contains <nan> values"
    y_num_classes, y_num_classes_counts = np.unique(Y, return_counts=True)
    print(f'[Train model] Number of unique label-values: {y_num_classes} with counts: {y_num_classes_counts}')
    assert len(y_num_classes) == model_config['n_classes'], "[WARNING] Number of unique labels do not match"

    """
        augment dataset with Elastic deformation [Simard2003] with a certain probability:

        Alternative:
        add one line in front of every model with augmentation operator:

            img_augmentation = Sequential(
            [
                preprocessing.RandomRotation(factor=0.15),
                preprocessing.RandomTranslation(height_factor=0.1, width_factor=0.1),
                preprocessing.RandomFlip(),
                preprocessing.RandomContrast(factor=0.1),
            ],
            name="img_augmentation",
            )

            x = img_augmentation(inputs)    

    """
    if config['use_elastic_transformation'] and augment_factor > 1:

        # calculate augmentation probability per sample via: augment_factor = 4 => 1 - (1/augment_factor) = 0.75
        augmentation_probability = (1 - (1 / augment_factor)) / 2
        seed = 42
        print(f'[Train model] Perform elastic transformation with probability per sample: {augmentation_probability}')

        # augment the training input and label images
        for idx, x_tmp in enumerate(x):
            x_tmp = np.squeeze(x_tmp)

            # if random value is within the probability-range: augment the actual image by elastic transformation
            if np.random.random() < augmentation_probability:
                x_train_transformed = utils.elastic_transform(x_tmp, seed=seed)

                # transform to required format: (batch, x, y, c)
                if len(x_train_transformed.shape) == 3:
                    # has already channel dimension
                    x_train_transformed = np.expand_dims(x_train_transformed, axis=0)
                else:
                    x_train_transformed = np.expand_dims(np.expand_dims(x_train_transformed, axis=-1), axis=0)

                x = np.concatenate((x, x_train_transformed), axis=0)
                Y.append(Y[idx])

    else:
        print('[Train model] Do <NOT> use elastic transformation')

    # Preprocessing of the input data (normalization)
    print('[Train model] image intensity min-max-range before preprocessing:', x.min(), x.max())
    x = utils.preprocessing(x, mode='zero_one')
    print('[Train model] image intensity min-max-range after preprocessing:', x.min(), x.max())

    # Preprocessing for the label data (transfer labels to categorical arrays (e.g. (0) -> [1,0] ; (1) -> [0,1] )
    y = tf.keras.utils.to_categorical(Y, num_classes=model_config['n_classes'])

    # split the data in train and test images
    x_train, x_valid, y_train, y_valid = train_test_split(x, y,
                                                          train_size=validation_split,
                                                          shuffle=True,
                                                          random_state=42)

    print('[Train model] Split data into training and validation data:')
    print('[Train model] Train data:\t', x_train.shape, y_train.shape)
    print('[Train model] Validation data:\t', x_valid.shape, y_valid.shape)

    # define the ImageDataGenerator to perform the data augmentation
    datagen = tf.keras.preprocessing.image.ImageDataGenerator(
        rotation_range=90,
        width_shift_range=0.1,
        height_shift_range=0.1,
        zoom_range=0.2,
        horizontal_flip=True,
        vertical_flip=True
    )

    # differ for data normalization
    # if normalization_mode == "zero_one":
    #     data_gen_args['rescale'] = 1./255
    # elif normalization_mode == "minus_one_to_one":
    #     data_gen_args['rescale'] = 1./(127.5-1)

    # provide the same seed and keyword arguments to the fit and flow methods
    seed = 42
    datagen.fit(x_train, seed=seed)

    # create generators for input image for training
    train_image_generator = datagen.flow(x_train, y_train, seed=seed)

    # create the log directory
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)
        print('[Train model] create directory folder for log:', log_dir)

    # get all callbacks which are active during the training
    training_callbacks = callbacks.get_callbacks(input_model_path=input_model_path, log_dir=log_dir)

    steps_epoch = x_train.shape[0] // batch_size
    print(f'[Train model] Number of steps per epoch-original: {steps_epoch}')
    steps_epoch = np.max([int(steps_epoch * config['augmentation_factor']), 1])
    print(f'[Train model] Number of steps per epoch-augmented: {steps_epoch}')

    # final data validation
    assert x_train.shape[1:] == model.input_shape[1:], "[WARNING] shapes of x-train and model-input do not match"
    assert x_valid.shape[1:] == model.input_shape[1:], "[WARNING] shapes of x-valid and model-input do not match"

    # fits the model on batches with real-time data augmentation:
    print('Start training ...')

    model.fit(train_image_generator,
              steps_per_epoch=steps_epoch,
              epochs=n_epochs,
              verbose=1,
              callbacks=list(training_callbacks.values()),
              validation_data=(x_valid, y_valid))

    # save the model, model-architecture and model-config
    utils.save_model_with_json(model=model,
                               model_path=output_model_path,
                               model_json_path=output_model_json_path,
                               config=config)

    # save the history plots
    if output_model_path:
        figure_path = output_model_path.split('/')[:-1]
        figure_path = '/'.join(figure_path)
        evaluate.plot_history(history=training_callbacks['history'], path=figure_path, model=model)
        # evaluate.plot_lr(history=training_callbacks['reduce_learning_rate'], path=figure_path)
        print('[Train model] Saved training history plots to:', figure_path)

    return model
