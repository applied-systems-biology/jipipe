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

import numpy as np
import pandas as pd
from glob import glob
import math
from sklearn.model_selection import train_test_split
from skimage import io
from pathlib import Path
import tensorflow as tf
from keras.preprocessing.image import ImageDataGenerator
from keras import callbacks
from dltoolbox import utils
from dltoolbox.evaluation import evaluate


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
    monitor_loss = config['monitor_loss']
    input_model_path = config["input_model_path"] if "input_model_path" in config else model_config['output_model_path']
    output_model_path = config['output_model_path']
    output_model_json_path = config['output_model_json_path']
    augment_factor = config['augmentation_factor']

    model = utils.load_and_compile_model(model_config, input_model_path, model)

    # read all filenames from input files and the corresponding labels table
    X_filenames = np.sort(glob(input_dir))
    Y_df = pd.read_csv(label_dir, index_col=0)

    X_paths = []
    Y = []

    # match input images with the labels table
    for label_filename, label_value in Y_df.iterrows():

        # discard objects where no label exist
        if math.isnan(label_value['label']):
            continue

        for input_filename in X_filenames:

            match = str(label_filename) in input_filename
            # if the filename of the image contains the label-filename the match condition is true
            if match:
                X_paths.append(input_filename)
                Y.append(label_value['label'])

                # print(f"label_filename: {label_filename} - input_filename: {input_filename} - match: {match}")
                break

    # read the matching input images and label values
    X = io.imread_collection(X_paths)

    print('[Train model] Input-images:', len(X), ', Label-values:', len(Y))
    print('[Train model] unique label values:', np.unique(Y, return_counts=True))

    assert len(X) == len(Y) > 0

    # split the data in train and test images
    x_train, x_valid, y_train, y_valid = train_test_split(X, Y,
                                                          train_size=validation_split,
                                                          shuffle=True)#,
                                                          #random_state=42)

    # transfer to numpy required arrays input: (height,width,3) -> (height,width,1)
    x_train, x_valid = np.array(x_train), np.array(x_valid)
    # transfer labels to categorical arrays (e.g. (0) -> [1,0] ; (1) -> [0,1] )
    y_train = tf.keras.utils.to_categorical(y_train, num_classes=model_config['n_classes'])
    y_valid = tf.keras.utils.to_categorical(y_valid, num_classes=model_config['n_classes'])

    # if input images are grayscaled images: add a pseudo-channel dimension
    while len(x_train.shape) < 4:
        x_train = np.expand_dims(np.array(x_train), axis=-1)
    while len(x_valid.shape) < 4:
        x_valid = np.expand_dims(np.array(x_valid), axis=-1)

    print('[Train model] Train data:\t', x_train.shape, y_train.shape)
    print('[Train model] Validation data:\t', x_valid.shape, y_valid.shape)

    # preprocessing: function that will be applied on each input image
    print('[Train model] image intensity min-max-range before preprocessing:', x_train.min(), x_train.max())
    x_train = utils.preprocessing(x_train, mode='zero_one')
    x_valid = utils.preprocessing(x_valid, mode='zero_one')
    print('[Train model] image intensity min-max-range after preprocessing:', x_train.min(), x_train.max())

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
    if augment_factor > 1:

        # calculate augmentation probability per sample via: augment_factor = 4 => 1 - (1/augment_factor) = 0.75
        augmentation_probability = 1 - (1 / augment_factor)
        seed = 42

        # augment the training input and label images
        for idx, x_tmp in enumerate(x_train):
            x_tmp = np.squeeze(x_tmp)

            # if random value is within the probability-range: augment the actual image by elastic transformation
            if np.random.random() < augmentation_probability:
                x_train_transformed = utils.elastic_transform(x_tmp, seed=seed)
                y_tmp = np.expand_dims(y_train[idx], axis=0)

                # transform to required format: (batch, x, y, c)
                x_train_transformed = np.expand_dims(np.expand_dims(x_train_transformed, axis=-1), axis=0)

                x_train = np.concatenate((x_train, x_train_transformed), axis=0)
                y_train = np.concatenate((y_train, y_tmp), axis=0)

                # print(x_train_transformed.shape, y_tmp.shape, y_tmp)
                # print(x_train.shape, y_train.shape)

        # augment the validation input and label images
        for idx, x_tmp in enumerate(x_valid):
            x_tmp = np.squeeze(x_tmp)

            # if random value is within the probability-range: augment the actual image by elastic transformation
            if np.random.random() < augmentation_probability:
                x_valid_transformed = utils.elastic_transform(x_tmp, seed=seed)
                y_tmp = np.expand_dims(y_valid[idx], axis=0)

                # transform to required format: (batch, x, y, c)
                x_valid_transformed = np.expand_dims(np.expand_dims(x_valid_transformed, axis=-1), axis=0)

                x_valid = np.concatenate((x_valid, x_valid_transformed), axis=0)
                y_valid = np.concatenate((y_valid, y_tmp), axis=0)

        print(
            f'[Train model] Augment dataset with probability per sample: {augmentation_probability} after elastic transformation with seed: {seed}')
        print('[Train model] Train data:\t', x_train.shape, y_train.shape)
        print('[Train model] Validation data:\t', x_valid.shape, y_valid.shape)

    ### define the ImageDataGenerator to perform the data augmentation
    datagen = ImageDataGenerator(
        rotation_range=120,
        width_shift_range=0.1,
        height_shift_range=0.1,
        zoom_range=0.2,
        horizontal_flip=True,
        vertical_flip=True)

    # provide the same seed and keyword arguments to the fit and flow methods
    seed = 42
    datagen.fit(x_train, seed=seed)

    # create generators for input image for training
    train_image_generator = datagen.flow(x_train, y_train, seed=seed)

    # create and define callback to monitor the training
    if monitor_loss not in ['loss', 'val_loss']:
        monitor_loss = 'val_loss'

    # TODO: callbacks über separaten node definieren in eigenem script + Tensorboard erstellen
    # erstelle { min , medium , max } callbacks, um nur einen parameter an dieser Stelle anzugeben
    # tbCallBack = callbacks.TensorBoard(log_dir=log_dir,
    #                                     histogram_freq=0,
    #                                     write_graph=False,
    #                                     write_images=True)
    earlyStopping = callbacks.EarlyStopping(monitor=monitor_loss,
                                            patience=200, verbose=1, mode='min')
    mcp_save = callbacks.ModelCheckpoint(input_model_path, save_best_only=True, monitor=monitor_loss, mode='min')
    reduce_lr = callbacks.ReduceLROnPlateau(monitor=monitor_loss, factor=0.85,
                                            patience=50, min_lr=0.000001, verbose=1)

    steps_epoch = x_train.shape[0] / batch_size
    print('[Train model] Number of steps per epoch-original:', steps_epoch)
    steps_epoch = int(steps_epoch * config['augmentation_factor'])
    print('[Train model] Steps per epoch-augmented:', steps_epoch)

    # fits the model on batches with real-time data augmentation:
    print('Start training ...')

    history = model.fit(train_image_generator,
                        steps_per_epoch=steps_epoch,
                        epochs=n_epochs,
                        verbose=1,
                        callbacks=[earlyStopping, mcp_save, reduce_lr],  # tbCallBack
                        validation_data=(x_valid, y_valid),
                        validation_steps=x_valid.shape[0] / batch_size)

    # save model, used config file and history plots
    if output_model_path:
        model.save(output_model_path)
        print('[Train model] Saved trained model to:', output_model_path)

        figure_path = output_model_path.split('/')[:-1]
        figure_path = '/'.join(figure_path)
        evaluate.plot_history(history=history, path=figure_path, model=model)
        print('[Train model] Saved training history plots to:', figure_path)

    if output_model_json_path:
        model_json = model.to_json()
        with open(output_model_json_path, "w") as f:
            f.write(model_json)
        print('[Train model] Saved trained model JSON to:', output_model_json_path)

        config_save_path = Path(output_model_json_path) / 'training_config.json'
        with open(config_save_path, "w") as f:
            f.write(config)
        print('[Train model] Save training config file JSON to:', config_save_path)

    return model
