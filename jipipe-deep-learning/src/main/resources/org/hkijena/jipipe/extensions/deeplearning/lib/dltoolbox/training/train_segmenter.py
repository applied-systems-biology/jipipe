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
from sklearn.model_selection import train_test_split
from skimage import io
from keras.preprocessing.image import ImageDataGenerator
from keras import callbacks
from dltoolbox.utils import load_and_compile_model


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

    model = load_and_compile_model(model_config, input_model_path, model)

    # validate input and label images
    X = io.imread_collection(input_dir)
    Y = io.imread_collection(label_dir)

    print('[Train model] Input-images:', len(X), ', Label-images:', len(Y))

    assert len(X) == len(Y) > 0

    # split the data in train and test images
    x_train, x_valid, y_train, y_valid = train_test_split(X, Y,
                                                          train_size=validation_split,
                                                          shuffle=True,
                                                          random_state=42)

    # transfer to numpy required arrays input: (height,width,3) -> (height,width,1)
    x_train, x_valid = np.array(x_train), np.array(x_valid)
    y_train, y_valid = np.array(y_train), np.array(y_valid)

    # TODO: Hier ein Validitätsvergleich durchführen, ob input/output = input/output von Model
    if len(y_train.shape) == 3:
        y_train = np.expand_dims(np.array(y_train), axis=-1)
    if len(y_valid.shape) == 3:
        y_valid = np.expand_dims(np.array(y_valid), axis=-1)

    print('[Train model] Train data:', x_train.shape, y_train.shape)
    print('[Train model] Validation data:', x_valid.shape, y_valid.shape)

    # TODO: geometrical transformation implementieren mit folgendem parameter:
    # preprocessing_function: function that will be applied on each input. 
    # The function will run after the image is resized and augmented. 
    # The function should take one argument: one image (Numpy tensor with rank 3), 
    # and should output a Numpy tensor with the same shape.

    data_gen_args = dict(
        rotation_range=120,
        width_shift_range=0.1,
        height_shift_range=0.1,
        zoom_range=0.2,
        horizontal_flip=True,
        vertical_flip=True)

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

    # TODO: callbacks über separaten node definieren in eigenem script
    # erstelle { min , medium , max } callbacks, um nur einen parameter an dieser Stelle anzugeben
    # tbCallBack = callbacks.TensorBoard(log_dir=log_dir,
    #                                     histogram_freq=0,
    #                                     write_graph=False,
    #                                     write_images=True)
    earlyStopping = callbacks.EarlyStopping(monitor=monitor_loss,
                                            patience=200, verbose=1, mode='min')
    # mcp_save = callbacks.ModelCheckpoint(os.path.join(mod_dir, 'mdl_wts.hdf5'),
    #                                     save_best_only=True, monitor=monitor_loss, mode='min')
    reduce_lr = callbacks.ReduceLROnPlateau(monitor=monitor_loss, factor=0.85,
                                            patience=50, min_lr=0.000001, verbose=1)

    steps_epoch = x_train.shape[0] / batch_size
    print('[Train model] Number of steps per epoch-original:', steps_epoch)
    steps_epoch = int(steps_epoch * config['augmentation_factor'])
    print('[Train model] Steps per epoch-augmented:', steps_epoch)

    # fits the model on batches with real-time data augmentation:
    print('Start training ...')

    # Warning: THIS FUNCTION IS DEPRECATED. It will be removed in a future version. Instructions for updating: Please use Model.fit, which supports generators.
    model.fit_generator(train_generator,
                        steps_per_epoch=steps_epoch,
                        epochs=n_epochs,
                        verbose=1,
                        # callbacks=[tbCallBack, earlyStopping, mcp_save, reduce_lr],
                        callbacks=[earlyStopping, reduce_lr],  # tbCallBack, mcp_save
                        validation_data=(x_valid, y_valid),
                        validation_steps=x_valid.shape[0] / batch_size)

    # Use model.fit for tensorflow > v2.1.0 
    # history = model.fit(
    #     x=train_generator,
    #     #y=train_label_generator,
    #     batch_size=batch_size,
    #     epochs=n_epochs,
    #     verbose=1,
    #     callbacks=[earlyStopping, reduce_lr], # tbCallBack, mcp_save
    #     validation_split=0.0,
    #     validation_data=(x_valid, y_valid),
    #     shuffle=True,
    #     class_weight=None,
    #     sample_weight=None,
    #     initial_epoch=0,
    #     steps_per_epoch=steps_epoch,
    #     validation_steps=None,
    #     #validation_batch_size=None,
    #     validation_freq=1,
    #     max_queue_size=10#,
    #     #workers=1#,
    #     #use_multiprocessing=False#,
    # )

    if output_model_path:
        model.save(output_model_path)
        print('[Train model] Saved trained model to:', output_model_path)

    if output_model_json_path:
        model_json = model.to_json()
        with open(output_model_json_path, "w") as f:
            f.write(model_json)
        print('[Train model] Saved trained model JSON to:', output_model_json_path)

    return model
