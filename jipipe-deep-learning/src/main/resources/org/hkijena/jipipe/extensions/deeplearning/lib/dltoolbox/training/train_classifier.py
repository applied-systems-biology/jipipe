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
from sklearn.utils import class_weight

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
    n_classes = model_config['n_classes']
    augment_factor = config['augmentation_factor']
    class_weight_factor = config['class_weight_factor']
    log_dir = config['log_dir']
    num_classes = model_config['n_classes']
    show_plots = config['show_plots']

    # load the model
    if model is not None:
        assert isinstance(model, tf.keras.models.Model)
        print(f'[Train model] Use model with input shape: {model.input_shape} and output shape: {model.output_shape}')
    else:
        model = utils.load_and_compile_model(model_config, input_model_path, model)
        print(f'[Train model] Model was successfully loaded from path: {input_model_path}')

    # check whether the label data are specified within a .csv table
    label_as_images = not str(label_dir).endswith('csv')
    assert not label_as_images, "Label format not valid: provide the label files within a .csv file"

    # read the input and label images in dependence of their specified format: directory or .csv-table
    X, filepath = utils.read_images(input_dir, model_input_shape=model.input_shape, read_input=True)
    Y = utils.read_labels(label_dir)

    print('[Train model] Input-images:', len(X), ', Label-values:', len(Y))

    # validate with one random sample to proof the sequence-match between input and labels
    if show_plots:
        rd_idx = np.random.randint(low=0, high=len(X))
        x_rd, y_rd = X[rd_idx], Y[rd_idx]
        utils.plot_image_with_label(img=x_rd, label=y_rd, index=rd_idx)

    assert len(X) == len(Y) > 0, "Unequal number of input - label images/values"

    # validate input data
    x = utils.validate_image_shape(model.input_shape, images=X)

    print('[Train model] Input data:', x.shape)

    assert not np.any(np.isnan(x)), "[WARNING] Input data contains <nan> values"
    y_num_classes, y_num_classes_counts = np.unique(Y, return_counts=True)
    print(f'[Train model] Number of unique label-values: {y_num_classes} with counts: {y_num_classes_counts}')
    assert len(y_num_classes) == num_classes, "[WARNING] Number of unique labels do not match"

    neg = y_num_classes_counts[0]
    pos = y_num_classes_counts[1]
    total = neg + pos

    ### for 2 classes: add the best derived bias value: b_0 = log(pos/neg) = log(num_ones/num_zeros):
    # Initial loss is about 50x less than with naive initialization. This way, the model not have spend first few epochs
    # just learning that positive examples are unlikely. This also makes it easier read the loss graphs during training.
    if len(y_num_classes) == 2:
        # initial setting of the bias so that the model makes more reasonable initial estimates
        initial_bias = np.log([pos / neg])
        output_bias = tf.keras.initializers.Constant(initial_bias)
        model.layers[-1].bias_initializer = output_bias
        print(f'[Train model] Training with 2 classes: pos: {pos} - neg: {neg} -> initial_bias: {initial_bias}')

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
    print('[Train model] Input image intensity min-max-range before preprocessing:', x.min(), x.max())
    if x.max() > 1:
        x = utils.preprocessing(x, mode=normalization_mode)
        print('[Train model] Input image intensity min-max-range after preprocessing:', x.min(), x.max())

    # Preprocessing for the label data (transfer labels to categorical arrays (e.g. (0) -> [1,0] ; (1) -> [0,1] )
    y = tf.keras.utils.to_categorical(Y, num_classes=num_classes)

    # Split into train - test data, in case no explicit validation data is specified
    if input_validation_dir and label_validation_dir:
        print('[Train model] Validation data is explicit given: no random split')

        x_train = x
        y_train = y

        # read validation data
        x_valid, filepath_x_valid = utils.read_images(path_dir=input_validation_dir,
                                                      model_input_shape=model.input_shape,
                                                      read_input=True)
        y_valid = utils.read_labels(path_dir=label_validation_dir)

        # validate validation data
        x_valid = utils.validate_image_shape(model.input_shape, images=x_valid)

        # Preprocessing validation data (normalization)
        print('[Train model] Validation input image intensity min-max-range before preprocessing:', x_valid.min(), x_valid.max())
        if x_valid.max() > 1:
            x_valid = utils.preprocessing(x_valid, mode=normalization_mode)
            print('[Train model] Validation input image intensity min-max-range after preprocessing:', x_valid.min(),
                  x_valid.max())

        # Preprocessing for the validation label data (transfer labels to categorical arrays (e.g. (0) -> [1,0] ; (1) -> [0,1] )
        y_valid = tf.keras.utils.to_categorical(y_valid, num_classes=num_classes)

    else:
        print('[Train model] Validation data is NOT explicit given: Split data into training and validation data')
        x_train, x_valid, y_train, y_valid = train_test_split(x, y,
                                                              train_size=validation_split,
                                                              shuffle=True,
                                                              random_state=42)

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
    training_callbacks = callbacks.get_callbacks(num_classes=n_classes,
                                                 input_model_path=input_model_path,
                                                 log_dir=log_dir,
                                                 unet_model=False,
                                                 show_plots=show_plots,
                                                 val_data=[x_valid, y_valid])

    steps_epoch = x_train.shape[0] // batch_size
    print(f'[Train model] Number of steps per epoch-original: {steps_epoch}')
    steps_epoch = np.max([int(steps_epoch * config['augmentation_factor']), 1])
    print(f'[Train model] Number of steps per epoch-augmented: {steps_epoch}')

    # final data validation
    assert x_train.shape[1:] == model.input_shape[1:], "[WARNING] shapes of x-train and model-input do not match"
    assert x_valid.shape[1:] == model.input_shape[1:], "[WARNING] shapes of x-valid and model-input do not match"

    ### for 2 classes add class weights: {0: None, 1: uniform, else: c_i=(1/num_zeros)*(num_all/class_weight_factor) }
    if len(y_num_classes) == 2:
        if class_weight_factor == 0:
            training_class_weights = None
        elif class_weight_factor == 1:
            training_class_weights = class_weight.compute_class_weight('balanced', y_num_classes, y.ravel())
        elif class_weight_factor > 1:
            # Scaling total/factor helps keep loss to similar magnitude. Sum of weights of all examples stays the same.
            weight_for_0 = (1 / neg) * (total / class_weight_factor)
            weight_for_1 = (1 / pos) * (total / class_weight_factor)

            training_class_weights = [weight_for_0, weight_for_1]
        else:
            training_class_weights = None
    else:
        training_class_weights = None
    print(f'[Train model] Binary class weights:', training_class_weights)

    # fits the model on batches with real-time data augmentation:
    print('Start training ...')

    model.fit(train_image_generator,
              steps_per_epoch=steps_epoch,
              epochs=n_epochs,
              verbose=1,
              callbacks=list(training_callbacks.values()),
              validation_data=(x_valid, y_valid),
              class_weight=training_class_weights)

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
        evaluate.plot_history(training_callbacks['history'], path=figure_path, model=model, show_plots=show_plots)
        # evaluate.plot_lr(history=training_callbacks['reduce_learning_rate'], path=figure_path)
        print('[Train model] Saved training history plots to:', figure_path)

    return model
