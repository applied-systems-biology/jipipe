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

os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"

import numpy as np
import json
from sklearn.model_selection import train_test_split
from keras.preprocessing.image import ImageDataGenerator
from keras import callbacks


# set the GPU where the underlying model will be trained on
def set_GPU_device(device_id):
    os.environ["CUDA_VISIBLE_DEVICES"] = str(device_id)
    print('os.environ["CUDA_VISIBLE_DEVICES"]:', os.environ["CUDA_VISIBLE_DEVICES"])

#############################################################
#           train a single segmentation network             #
#############################################################
def train_model(model, X, Y, config, verbose):

    ### assign hyper-parameter for training procedure
    n_epochs = config['max_epochs'][0]
    batch_size = config['batch_size'][0]
    validation_split = config['validation_split'][0]
    monitor_loss = config['monitor_loss'][0]
    
    # if verbose > 0:
    #     [ print(parameter,':\t', value_desc) for parameter, value_desc in config.items() ]

    # validate input and label images
    if verbose > 0:
        print('\n\tinput-images:', len(X), 'label-images:', len(Y) )

    assert len(X) == len(Y) > 0

    # split the data in train and test images
    x_train, x_valid, y_train, y_valid = train_test_split(X, Y, 
                                                            train_size=validation_split, 
                                                            shuffle=True, 
                                                            random_state=42)

    # tranfer to numpy required arrays input: (height,width,3) -> (height,width,1)
    x_train, x_valid = np.array(x_train), np.array(x_valid)

    # TODO: Hier ein Validitätsvergleich durchführen, ob input/output = input/output von Model
    if len(y_train.shape) == 3:
        y_train = np.expand_dims(np.array(y_train), axis=-1)
    if len(y_valid.shape) == 3:
        y_valid = np.expand_dims(np.array(y_valid), axis=-1)

    if verbose > 0:
        print('\n\ttrain data:', x_train.shape, y_train.shape)
        print('\tvalid data:', x_valid.shape, y_valid.shape)

    # TODO: geometrical transformation implementieren mit folgendem parameter:
    # preprocessing_function: function that will be applied on each input. 
    # The function will run after the image is resized and augmented. 
    # The function should take one argument: one image (Numpy tensor with rank 3), 
    # and should output a Numpy tensor with the same shape.

    data_gen_args = dict(#rescale=1./255, # TODO: dieser ververarbeitungs Schritt wird von einer node innerhalb JIPipe erledigt
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

    # TODO: dieser ververarbeitungs Schritt wird von einer node innerhalb JIPipe erledigt
    # for write_images in TensorBoard, validation data must be provided without generator:
    # x_valid = utils.preprocessing(x_valid)
    # y_valid = utils.preprocessing(y_valid)
    
    # TODO: das hier wird von einer separaten node gemacht
    # store hyperparameter to JSON
    # json_path = os.path.join(mod_dir, 'mdl_hyperparameter.json')
    # with open(json_path, 'w') as f:
    #     json.dump(config, f)
    
    # TODO: das hier wird von einer separaten node gemacht
    # store model-keras-parameter to JSON
    # model_json = unet.to_json()
    # json_path = os.path.join(mod_dir, 'mdl_wts.json')
    # with open(json_path, "w") as json_file:
    #     json_file.write(model_json)

    # if verbose > 0:
    #     print('save model-hyperparameter to json:', json_path)
    #     print('save model-info to json:', json_path)
    
    # create and define callback to monitor the training
    if monitor_loss not in [ 'loss' , 'val_loss' ]:
        monitor_loss = 'val_loss'

    if verbose > 0:
        print('\nmonitor-loss for training callbacks:', monitor_loss)

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
    print('\nsteps per epoch-original:', steps_epoch)
    steps_epoch = int(steps_epoch * config['augmentation_factor'][0])
    print('steps per epoch-augmented:', steps_epoch)
        
    # fits the model on batches with real-time data augmentation:
    print('Start training ...')
    
    # Warning: THIS FUNCTION IS DEPRECATED. It will be removed in a future version. Instructions for updating: Please use Model.fit, which supports generators.
    model.fit_generator(train_generator,
            steps_per_epoch=steps_epoch,
            epochs=n_epochs,
            verbose=1,
            #callbacks=[tbCallBack, earlyStopping, mcp_save, reduce_lr],
            callbacks=[earlyStopping, reduce_lr], # tbCallBack, mcp_save
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

    #return history






# test = {
#     "input_dir": ["/asbdata/Philipp/FattyTissueProportion/subwindows_512x512_v3/original/", "path to folder containing input images"],
#     "label_dir": ["/asbdata/Philipp/FattyTissueProportion/subwindows_512x512_v3/labels/", "path to folder containing output images"],
#     "data_info": ["/asbdata/Philipp/FattyTissueProportion/subwindows_512x512_v3/subwindows_512x512_creation.csv", "csv-file where information about the images are stored"],
#     "checkpoint_dir": ["./", "where to put model and log files"],
#     "max_epochs": [5, "number of training epochs"],
#     "kernel_size": [7, "kernel-size for the convolutional filter"],
#     "regularization": ["GaussianDropout", 0.1, "regularization method within the architecture - second parameter specifies the rate/std - possible values: {none,Dropout,GaussianDropout,GaussianNoise}"],
#     "batch_size": [32, "number of images in batch"],
#     "n_filters": [64, "number of filters in first conv layer"],
#     "img_size": [512, "crop images to this size before for training input"],
#     "augmentation_factor": [5, "factor to augment the data by {flip, rotate, shift} the batch-image, 1 means no augmentation"],
#     "learning_rate": [0.0001, "initial learning rate for adam"],
#     "beta1": [0.5, "momentum term of adam"],
#     "monitor_loss":["val_loss", "select the parameters which are valid to monitor during the training: { loss , val_loss }" ],
#     "n_classes": [2, "number of classes for distingish semantic segmentation"],
#     "validation_split": [0.85, "percentage which is used for validation (e.g.: 0.8 => 80% training, 20% test"],
#     "segmentation_threshold": [0.9, "binary threshold for set the black-white output of the network to rest OR intected tissue"],
#     "num_K_folds": [9, "number of K-fold cross-validation"]
# }


#from ..models.SegNet import *
# import sys
# sys.path.insert(0, '../models/')
# from SegNet import build_model

# mymodel = build_model((256,256,3))

# x = np.ones(shape=(20,256,256,3))
# y = np.ones(shape=(20,256,256))*3
# train_model(mymodel, x, y, test, 1)



