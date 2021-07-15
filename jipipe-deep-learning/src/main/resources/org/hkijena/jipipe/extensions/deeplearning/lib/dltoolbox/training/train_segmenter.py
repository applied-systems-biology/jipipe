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
from sklearn.model_selection import train_test_split
from skimage import io
from pathlib import Path
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from keras import callbacks
# from tensorflow.keras import callbacks
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

    # Check for tif images
    input_is_tifffile = utils.check_for_tif_files(input_dir)
    label_is_tifffile = utils.check_for_tif_files(label_dir)

    # validate input and label images, depending on tif-files
    if input_is_tifffile:
        X = io.imread_collection(input_dir, plugin='tifffile')
    else:
        X = io.imread_collection(input_dir)
    if label_is_tifffile:
        Y = io.imread_collection(label_dir, plugin='tifffile')
    else:
        Y = io.imread_collection(label_dir)

    print('[Train model] Input-images:', len(X), ', Label-images:', len(Y))

    assert len(X) == len(Y) > 0

    # split the data in train and test images
    x_train, x_valid, y_train, y_valid = train_test_split(X, Y,
                                                          train_size=validation_split,
                                                          shuffle=True,
                                                          random_state=42)

    # validate input data
    x_train = utils.validate_image_shape(model.input_shape, images=x_train)
    x_valid = utils.validate_image_shape(model.input_shape, images=x_valid)

    # validate label data
    y_train = utils.validate_image_shape(model.output_shape, images=y_train)
    y_valid = utils.validate_image_shape(model.output_shape, images=y_valid)

    # transfer to numpy required arrays input: (height,width,3) -> (height,width,1)
    # x_train, x_valid = np.array(x_train), np.array(x_valid)
    # y_train, y_valid = np.array(y_train), np.array(y_valid)

    # if necessary: add <channel> - dimension
    # while len(x_train.shape) < 4:
    #     x_train = np.expand_dims(np.array(x_train), axis=-1)
    # while len(x_valid.shape) < 4:
    #     x_valid = np.expand_dims(np.array(x_valid), axis=-1)
    #
    # while len(y_train.shape) < 4:
    #     y_train = np.expand_dims(np.array(y_train), axis=-1)
    # while len(y_valid.shape) < 4:
    #     y_valid = np.expand_dims(np.array(y_valid), axis=-1)

    print('[Train model] Train data:\t', x_train.shape, y_train.shape)
    print('[Train model] Validation data:\t', x_valid.shape, y_valid.shape)

    return

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
    if config['use_elastic_transformation'] and  augment_factor > 1:

        # calculate augmentation probability per sample via: augment_factor = 4 => 1 - (1/augment_factor) = 0.75
        augmentation_probability = (1 - (1/augment_factor)) / 2
        seed = 42

        # augment the training input and label images
        for idx, x_tmp in enumerate(x_train):
            x_tmp = np.squeeze(x_tmp)

            # if random value is within the probability-range: augment the actual image by elastic transformation
            if np.random.random() < augmentation_probability:

                x_train_transformed = utils.elastic_transform(x_tmp, seed=seed)
                y_train_transformed = utils.elastic_transform(np.squeeze(y_train[idx]), seed=seed)

                # transform to required format: (batch, x, y, c)
                x_train_transformed = np.expand_dims(np.expand_dims(x_train_transformed, axis=-1), axis=0)
                y_train_transformed = np.expand_dims(np.expand_dims(y_train_transformed, axis=-1), axis=0)

                x_train = np.concatenate((x_train, x_train_transformed), axis=0)
                y_train = np.concatenate((y_train, y_train_transformed), axis=0)

        # augment the validation input and label images
        for idx, x_tmp in enumerate(x_valid):
            x_tmp = np.squeeze(x_tmp)

            # if random value is within the probability-range: augment the actual image by elastic transformation
            if np.random.random() < augmentation_probability:

                x_valid_transformed = utils.elastic_transform(x_tmp, seed=seed)
                y_valid_transformed = utils.elastic_transform(np.squeeze(y_valid[idx]), seed=seed)

                # transform to required format: (batch, x, y, c)
                x_valid_transformed = np.expand_dims(np.expand_dims(x_valid_transformed, axis=-1), axis=0)
                y_valid_transformed = np.expand_dims(np.expand_dims(y_valid_transformed, axis=-1), axis=0)

                x_valid = np.concatenate((x_valid, x_valid_transformed), axis=0)
                y_valid = np.concatenate((y_valid, y_valid_transformed), axis=0)

        print(f'[Train model] Augment dataset with probability per sample: {augmentation_probability} after elastic transformation with seed: {seed}')
        print('[Train model] Train data:\t', x_train.shape, y_train.shape)
        print('[Train model] Validation data:\t', x_valid.shape, y_valid.shape)

    else:
        print('[Train model] Do NOT use elastic transformation')

    # Preprocessing of the input data (normalization)
    x_train = utils.preprocessing(x_train, mode=config['normalization'])
    x_valid = utils.preprocessing(x_valid, mode=config['normalization'])

    # Preprocessing for the label data (normalization)
    y_train = y_train / y_train.max()
    y_valid = y_valid / y_valid.max()

    # print(x_train.min(), x_train.max(), x_valid.min(), x_valid.max())
    # print(y_train.min(), y_train.max(), y_valid.min(), y_valid.max())
    # return

    data_gen_args = dict(
        rotation_range=120,
        width_shift_range=0.1,
        height_shift_range=0.1,
        zoom_range=0.2,
        horizontal_flip=True,
        vertical_flip=True
    )

    train_image_datagen = ImageDataGenerator(**data_gen_args)
    train_label_datagen = ImageDataGenerator(**data_gen_args)

    # provide the same seed and keyword arguments to the fit and flow methods
    seed = 42
    train_image_datagen.fit(x_train, augment=True, seed=seed)
    train_label_datagen.fit(y_train, augment=True, seed=seed)

    # combine generators into one which yields image and masks for training and validation
    train_image_generator = train_image_datagen.flow(x_train, seed=seed)
    train_label_generator = train_label_datagen.flow(y_train, seed=seed)

    train_generator = zip(train_image_generator, train_label_generator)

    # create and define callback to monitor the training
    if monitor_loss not in ['loss', 'val_loss']:
        monitor_loss = 'val_loss'

    if not os.path.exists(config['log_dir']):
        os.makedirs(config['log_dir'])
        print('[Evaluate] create directory folder for log:', config['log_dir'])

    # TODO: callbacks über separaten node/skript definieren in eigenem script + Tensorboard erstellen
    # TODO: tensorboard callback zum laufen bringen
    tbCallBack = callbacks.TensorBoard(log_dir=config['log_dir'],
                                        histogram_freq=0,
                                        write_graph=False,
                                        write_images=True)
    earlyStopping = callbacks.EarlyStopping(monitor=monitor_loss,
                                            patience=200, verbose=1, mode='min')
    mcp_save = callbacks.ModelCheckpoint(input_model_path, save_best_only=True, monitor=monitor_loss, mode='min')
    reduce_lr = callbacks.ReduceLROnPlateau(monitor=monitor_loss, factor=0.85,
                                            patience=50, min_lr=0.000001, verbose=1)
    csv_logger = callbacks.CSVLogger(os.path.join(config['log_dir'], 'training.log'), separator=',', append=False)
    history = callbacks.History()


    steps_epoch = x_train.shape[0] / batch_size
    print('[Train model] Number of steps per epoch-original:', steps_epoch)
    steps_epoch = int(steps_epoch * augment_factor)
    print('[Train model] Steps per epoch-augmented:', steps_epoch)

    # fits the model on batches with real-time data augmentation:
    print('Start training ...')

    model.fit_generator(train_generator,
                        steps_per_epoch=steps_epoch,
                        epochs=n_epochs,
                        verbose=1,
                        callbacks=[earlyStopping, mcp_save, reduce_lr, csv_logger, history], #tbCallBack
                        validation_data=(x_valid, y_valid))

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

        config_save_path = output_model_json_path.replace(os.path.basename(output_model_json_path), '')
        config_save_path = os.path.join(config_save_path, 'training_config.json')
        with open(config_save_path, "w") as f:
            f.write(config)
        print('[Train model] Save training config file JSON to:', config_save_path)

    return model
