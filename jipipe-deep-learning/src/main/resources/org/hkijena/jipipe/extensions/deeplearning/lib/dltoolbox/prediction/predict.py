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

Script to predict a network
"""

import os

os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"

from dltoolbox import utils


# import json
# from glob import glob
# import time
# from datetime import datetime
# import cv2
# import numpy as np
# import skimage
# from scipy import ndimage
# import tifffile as tif
# import matplotlib.pyplot as plt
# import matplotlib as mpl
# import matplotlib.image as mpimg
# import pandas as pd


# set the GPU where the underlying model will be trained on
def set_GPU_device(device_id):
    os.environ["CUDA_VISIBLE_DEVICES"] = str(device_id)
    print('os.environ["CUDA_VISIBLE_DEVICES"]:', os.environ["CUDA_VISIBLE_DEVICES"])


#############################################################
#            predict / evaluate a single network            #
#############################################################


### model to evalute images with given chars in 'abbrevation' ### 
def evaluate_model(model, X, Y):  # #X_Y_path):

    # read ALL images path
    # original_path_all = np.sort( glob( os.path.join( input_path, 'original', '*.png') ) ) 
    # labels_path_all = np.sort( glob( os.path.join( input_path, 'labels', '*.png') ) )
    # print('[INFO] total images in directory - originals: {0} and labels: {1}'.format(len(original_path_all), len(labels_path_all)) )

    # X_test_path, Y_test_path = X_Y_path

    # print('\n\tX_test_path:', len(X_test_path), 'Y_test_path:', len(Y_test_path))        

    # read all test images
    # X_test_images = skimage.io.ImageCollection(X_test_path)
    # Y_test_images = skimage.io.ImageCollection(Y_test_path)
    # print('\n\tX_test_images:', len(X_test_images), 'Y_test_images:', len(Y_test_images) )

    print('\n\tX:', len(X), 'Y:', len(Y))

    # tranfer to numpy required arrays
    # x_test, y_test = np.array(X_test_images), np.array(Y_test_images)
    x_test, y_test = np.array(X), np.array(Y)
    y_test = np.expand_dims(np.array(y_test), axis=-1)

    # TODO: dieser ververarbeitungs Schritt wird von einer node innerhalb JIPipe erledigt
    # normalize input data
    # x_test = utils.preprocessing(x_test)

    print('\n\ttest data:', x_test.shape, y_test.shape, '\n\n')

    scores = model.evaluate(x_test, y_test, verbose=1)

    print(f'Score: {model.metrics_names[0]} of {scores[0]} ;\t {model.metrics_names[1]} of {scores[1]}')

    return scores


### predict a whole sample ###
"""
CAUTION: savePath should NOT contain the file ending (e.g. .tif / .png)
"""


def predict_sample(model, img_origin, savePath, save_probMap_image, verbose=0):
    winW, winH = config['img_size'][0], config['img_size'][0]
    windowStep = config['img_size'][0]
    mythresh = config["segmentation_threshold"][0]

    # create an empty image with the size of the original one, if choosen
    if model.output_shape[-1] == 3:
        img_prediction = np.zeros(img_origin.shape)
    else:
        img_prediction = np.zeros(img_origin.shape[:2])
    img_prediction_thresholded = np.zeros(img_prediction.shape)

    # TODO: das hier testen: ein großes Bild wird mit einem mal predicted:
    # muss im format (batches, x, y, channel) sein
    try:
        img_origin = np.expand_dims(img_origin, axis=0)
        img_prediction = model.predict(img_origin, batch_size=1)
        print('predict whole sample')
    except:

        print('predict sample via sliding-window operation')

        start_time = datetime.now()

        # iterate about each window
        for (row, col, window) in utils.sliding_window(img_origin, stepSize=windowStep, windowSize=(winW, winH)):

            y1, y2 = col, col + windowStep
            x1, x2 = row, row + windowStep

            # check if all values in the window equal
            if len(np.unique(window)) == 1:
                continue

            # padding if window has not the required shape (e.g. 256²), necessary for NN
            if window.shape[0] != winH or window.shape[1] != winW:
                window = utils.batchPadding(window, winW, int(np.max(window)))

            # normalize between [ 0, 1 ]
            window = utils.preprocessing(window)

            # check if the underlying cut-out is a 'batch'
            if len(window.shape) == 3:
                window = np.expand_dims(window, axis=0)

            # y_predict = model.predict_on_batch( np.array( [window] ) )
            y_predict = model.predict_on_batch([window])

            # segment image by binarize it with specified threshold and place it in corresponding big image
            y_binarized = y_predict > mythresh
            y_binarized = np.squeeze(y_binarized)

            tmp_shape = img_prediction_thresholded[y1:y2, x1:x2].shape
            img_prediction_thresholded[y1:y2, x1:x2] = y_binarized[:tmp_shape[0], :tmp_shape[1]]

            # plot image for (1) verbose activated (2) a positive segmentation and (3) 'medium' variance for the input image
            if verbose > 1 and np.max(y_predict) > mythresh and ndimage.variance(window) > 10:
                # plot segmentation just for black-white images
                fig, ax_arr = plt.subplots(1, 2, sharex=True, sharey=True, figsize=(15, 12))
                ax1, ax2 = ax_arr.ravel()

                ax1.imshow(np.squeeze(window))
                ax1.contour(y_binarized.astype(np.uint8), colors='green', linewidths=2)
                ax1.set_title("Origin image", fontsize=15)

                ax2.imshow(np.squeeze(y_predict[0]), cmap="gray")
                ax2.set_title("Network-prediction", fontsize=15)
                plt.show()

                print("PREDICTION: [prediction- shape/min/mean/max: {} / {:3.4f} / {:3.4f} / {:3.4f}]"
                      .format(y_predict[0].shape, np.min(y_predict[0]), np.mean(y_predict[0]), np.max(y_predict[0])))

            # build the whole img_prediction image to give a probability-prediciton of the model about the whole image
            y_predict = np.squeeze(y_predict)

            # cut-out the prediction-image if necessary (img_prediction[ y1:y2 , x1:x2 ] is at the frame border)
            cutout_shape = img_prediction[y1:y2, x1:x2].shape
            if cutout_shape != np.squeeze(y_predict).shape:
                y_predict = y_predict[:cutout_shape[0], :cutout_shape[1]]
                # print('cut-out the prediction', cutout_shape, y_predict.shape)

            if len(y_predict.shape) == 3:
                img_prediction[y1:y2, x1:x2, :] = y_predict
            else:
                img_prediction[y1:y2, x1:x2] = np.squeeze(y_predict)

            if verbose > 1:
                print('coordinate (x/y):', x1, y1)

        img_prediction_thresholded = img_prediction_thresholded.astype(np.uint8) * 255

        # save the whole (thresholded) image with all the annotations
        if savePath is None:
            print('skip storing of a whole prediction image')
        else:
            tif.imsave(savePath + '_threshold.tif', img_prediction_thresholded)
            print('save thresholded-image to:', savePath + '_threshold.tif')

            if save_probMap_image:
                tif.imsave(savePath + '.tif', img_prediction)
                print('save prediction-image to:', savePath + '_probMap.tif')

        time_elapsed = datetime.now() - start_time
        print('\nFinish prediciton in (hh:mm:ss.ms) {}'.format(time_elapsed))

    if verbose > 0:
        print('[INFO] finish create empty prediction image:', img_prediction.shape, img_prediction_thresholded.shape)

    return img_prediction_thresholded
