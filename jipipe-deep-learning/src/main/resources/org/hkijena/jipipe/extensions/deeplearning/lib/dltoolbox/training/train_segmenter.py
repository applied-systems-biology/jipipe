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
import tensorflow as tf
from sklearn.model_selection import train_test_split

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
    input_validation_dir = config['input_validation_dir']
    label_validation_dir = config['label_validation_dir']
    normalization_mode = config['normalization']
    n_epochs = config['max_epochs']
    batch_size = config['batch_size']
    validation_split = config['validation_split']
    input_model_path = config["input_model_path"] if "input_model_path" in config else model_config['output_model_path']
    output_model_path = config['output_model_path']
    output_model_json_path = config['output_model_json_path']
    augment_factor = config['augmentation_factor']
    log_dir = config['log_dir']

    # load the model
    if model is not None:
        assert isinstance(model, tf.keras.models.Model)
        print(f'[Train model] Use model with input shape: {model.input_shape} and output shape: {model.output_shape}')
    else:
        model = utils.load_and_compile_model(model_config, input_model_path, model)
        print(f'[Train model] Model was successfully loaded from path: {input_model_path}')

    # read the input and label images in dependence of their specified format: directory or .csv-table
    X = utils.read_images(input_dir, model_input_shape=model.input_shape,
                          read_input=True, labels_for_classifier=False)
    Y = utils.read_images(label_dir, model_input_shape=model.output_shape,
                          read_input=False, labels_for_classifier=False)

    print('[Train model] Input-images:', len(X), ', Label-images:', len(Y))

    # validate with 1 random sample, that the sequence between input and labels match
    if False:
        rd_idx = np.random.randint(low=0, high=len(X))
        x, y = X[rd_idx], Y[rd_idx]
        utils.plot_window(img=x, img_binary=y, title=f'Read images: validate sequence at index: {rd_idx}')

    assert len(X) == len(Y) > 0, "Unequal number of input - label images/values"

    # validate input data
    x = utils.validate_image_shape(model.input_shape, images=X)

    # validate label data
    y = utils.validate_image_shape(model.output_shape, images=Y)

    print('[Train model] Input data:', x.shape)
    print('[Train model] Label data:', y.shape)

    # Further data validation
    assert not np.any(np.isnan(x)), "[WARNING] Input data contains <nan> values"
    assert not np.any(np.isnan(y)), "[WARNING] Label data contains <nan> values"
    y_num_classes, y_num_classes_counts = np.unique(y, return_counts=True)
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

        # augment the input and label images
        for idx, x_tmp in enumerate(x):
            x_tmp = np.squeeze(x_tmp)

            # if random value is within the probability-range: augment the actual image by elastic transformation
            if np.random.random() < augmentation_probability:

                x_train_transformed = utils.elastic_transform(x_tmp, seed=seed)
                y_train_transformed = utils.elastic_transform(np.squeeze(y[idx]), seed=seed)

                # transform to required format: (batch, x, y, c)
                if len(x_train_transformed.shape) == 3:
                    # has already channel dimension
                    x_train_transformed = np.expand_dims(x_train_transformed, axis=0)
                else:
                    x_train_transformed = np.expand_dims(np.expand_dims(x_train_transformed, axis=-1), axis=0)

                y_train_transformed = np.expand_dims(np.expand_dims(y_train_transformed, axis=-1), axis=0)

                x = np.concatenate((x, x_train_transformed), axis=0)
                y = np.concatenate((y, y_train_transformed), axis=0)

        print('[Train model] Input data:\t', x.shape)
        print('[Train model] Label data:\t', y.shape)

    else:
        print('[Train model] Do <NOT> use elastic transformation')

    # Preprocessing of the input data (normalization)
    print('[Train model] Input image intensity min-max-range before preprocessing:', x.min(), x.max())
    if x.max() > 1:
        x = utils.preprocessing(x, mode=normalization_mode)
        print('[Train model] Input image intensity min-max-range after preprocessing:', x.min(), x.max())

    # Preprocessing of the label data (normalization)
    print('[Train model] Label image intensity min-max-range before preprocessing:', y.min(), y.max())
    if y.max() > 1:
        y = utils.preprocessing(y, mode=normalization_mode)
        print('[Train model] Label image intensity min-max-range after preprocessing:', y.min(), y.max())

    # Split into train - test data, in case no explicit validation data is specified
    if input_validation_dir and label_validation_dir:
        print('[Train model] Validation data is explicit given: no random split')

        x_train = x
        y_train = y

        # read validation data
        x_valid = utils.read_images(path_dir=input_validation_dir, model_input_shape=True, labels_for_classifier=False)
        y_valid = utils.read_images(path_dir=label_validation_dir, model_input_shape=False, labels_for_classifier=False)

        # validate validation data
        x_valid = utils.validate_image_shape(model.input_shape, images=x_valid)
        y_valid = utils.validate_image_shape(model.output_shape, images=y_valid)

        # Preprocessing validation data (normalization)
        print('[Train model] Validation input image intensity min-max-range before preprocessing:', x_valid.min(), x_valid.max())
        if x_valid.max() > 1:
            x_valid = utils.preprocessing(x_valid, mode=normalization_mode)
            print('[Train model] Validation input image intensity min-max-range after preprocessing:', x_valid.min(), x_valid.max())

        print('[Train model] Validation label image intensity min-max-range before preprocessing:', y_valid.min(), y_valid.max())
        if y_valid.max() > 1:
            y_valid = utils.preprocessing(y_valid, mode=normalization_mode)
            print('[Train model] Validation label image intensity min-max-range after preprocessing:', y_valid.min(), y_valid.max())

    else:
        print('[Train model] Validation data is NOT explicit given: Split data into training and validation data')
        x_train, x_valid, y_train, y_valid = train_test_split(x, y, test_size=1-validation_split, shuffle=True)

    print('[Train model] train data:\t', x_train.shape, y_train.shape, np.unique(y_train))
    print('[Train model] validation data:\t', x_valid.shape, y_valid.shape, np.unique(y_valid))

    data_gen_args = dict(
        rotation_range=90,
        width_shift_range=0.1,
        height_shift_range=0.1,
        zoom_range=0.2,
        horizontal_flip=True,
        vertical_flip=True,
    )

    # differ for data normalization
    # if normalization_mode == "zero_one":
    #     data_gen_args['rescale'] = 1./255
    # elif normalization_mode == "minus_one_to_one":
    #     data_gen_args['rescale'] = 1./(127.5-1)

    # combine generators into one which yields image and masks for input and label images for training purpose
    train_image_datagen = tf.keras.preprocessing.image.ImageDataGenerator(**data_gen_args)
    train_label_datagen = tf.keras.preprocessing.image.ImageDataGenerator(**data_gen_args)

    # provide the same seed and keyword arguments to the fit and flow methods
    seed = 42
    train_image_datagen.fit(x_train, augment=True, seed=seed)
    train_label_datagen.fit(y_train, augment=True, seed=seed)

    train_image_generator = train_image_datagen.flow(x_train, seed=seed)
    train_label_generator = train_label_datagen.flow(y_train, seed=seed)
    train_generator = zip(train_image_generator, train_label_generator)

    # create the log directory
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)
        print('[Train model] create directory folder for log:', log_dir)

    # get all callbacks which are active during the training
    training_callbacks = callbacks.get_callbacks(input_model_path=input_model_path, log_dir=log_dir)

    # calculate the augmented number of steps per epoch for the training and validation
    steps_epoch = x_train.shape[0] // batch_size
    print(f'[Train model] Number of steps per epoch-original: {steps_epoch}')
    steps_epoch = np.max([int(steps_epoch * augment_factor), 1])
    print(f'[Train model] Number of steps per epoch-augmented: {steps_epoch}')

    # final data validation
    assert x_train.shape[1:] == model.input_shape[1:], "[WARNING] shapes of x-train and model-input do not match"
    assert x_valid.shape[1:] == model.input_shape[1:], "[WARNING] shapes of x-valid and model-input do not match"
    assert y_train.shape[1:] == model.output_shape[1:], "[WARNING] shapes of y-train and model-output do not match"
    assert y_valid.shape[1:] == model.output_shape[1:], "[WARNING] shapes of y-valid and model-output do not match"

    # fits the model on batches with real-time data augmentation:
    print('Start training ...')

    model.fit(train_generator,
              steps_per_epoch=steps_epoch,
              epochs=n_epochs,
              verbose=1,
              callbacks=list(training_callbacks.values()),
              validation_data=(x_valid, y_valid))

    # save the model, model-architecture and model-config
    utils.save_model_with_json(model=model,
                               model_path=output_model_path,
                               model_json_path=output_model_json_path,
                               model_config=model_config,
                               operation_config=config)

    # save the history plots
    if output_model_path:
        figure_path = output_model_path.split('/')[:-1]
        figure_path = '/'.join(figure_path)
        evaluate.plot_history(history=training_callbacks['history'], path=figure_path, model=model)
        # evaluate.plot_lr(history=training_callbacks['reduce_learning_rate'], path=figure_path)
        print('[Train model] Saved training history plots to:', figure_path)

    return model
