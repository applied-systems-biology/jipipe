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
"""

import sys
import os
import numpy as np
import json
import math
import pandas as pd
from glob import glob
from pathlib import Path
import matplotlib.pyplot as plt
import tifffile
from tqdm import tqdm
from skimage import io, filters, img_as_float32, img_as_ubyte
from sklearn import metrics
from dltoolbox import utils


def plot_history(history, path, model):
    """
    Plot history of metrics

    Args:
        history: history object from model.fit
        path: save path where the plots will be stored

    Returns:

    """

    # Plot training & validation loss values
    figure = plt.figure(figsize=(12, 10))
    plt.plot(history.history[model.metrics_names[0]], linewidth=2)
    plt.plot(history.history[f'val_{model.metrics_names[0]}'], linewidth=2)
    plt.title(f'Model {model.metrics_names[0]}')
    plt.ylabel(f'{model.metrics_names[0]}')
    plt.xlabel('epoch')
    plt.legend(['Train', 'Validation'], loc='upper left')
    plt.savefig(os.path.join(path, f'training_validation_{model.metrics_names[0]}.png'))
    plt.show()

    # if additional metrics are given
    if len(model.metrics_names) > 1:
        # Plot training & validation accuracy values
        figure = plt.figure(figsize=(12, 10))
        plt.plot(history.history[model.metrics_names[1]], linewidth=2)
        plt.plot(history.history[f'val_{model.metrics_names[1]}'], linewidth=2)
        plt.title(f'Model {model.metrics_names[1]}')
        plt.ylabel(f'{model.metrics_names[1]}')
        plt.xlabel('epoch')
        plt.legend(['Train', 'Validation'], loc='upper left')
        plt.savefig(os.path.join(path, f'training_validation_{model.metrics_names[1]}.png'))
        plt.show()


def plot_probabilities(config):
    """
    Plot all probabilities for given samples

    Args:
        config: Evaluation parameters with it's function

    Returns:

    """

    input_dir = config['input_dir']
    output_figure_path = config['output_figure_path']

    # Get the raw images
    X_path = glob(input_dir)

    print('[Plot probabilities] available input images:', len(X_path))

    model_threshold = 0.5

    for i, x_path in enumerate(X_path):
        print(f'[Plot probabilities] nr: {i} / {len(X_path)} from path: {str(x_path)}')

        # Load the raw images
        image = io.imread(x_path)
        print('[Plot probabilities] Image to collect probabilities:', image.shape)

        probs = image.flatten()
        print(f'[Plot probabilities] # of prob.-values: {len(probs)} min-value: {probs.min()} max-value: {probs.max()}')

        # filter too low probability values
        filter_min_value = 0.05
        probs = np.array(probs)
        probs = probs[probs >= filter_min_value]
        print(f'[Plot probabilities] # of prob.-values: {len(probs)} min-value: {probs.min()} max-value: {probs.max()}')

        # calculate an otsu-threshold
        thresh = filters.threshold_otsu(np.array(probs))
        print(f'[Plot probabilities] own chosen threshold: {model_threshold}\tVS\totsu-threshold: {thresh}')

        if not os.path.exists(output_figure_path):
            os.makedirs(output_figure_path)
            print('[Plot probabilities] create directory folder:', output_figure_path)

        plt.hist(probs)
        plt.title('Probability histogram', fontsize=20)
        plt.axvline(model_threshold, color='r', linewidth=4)
        plt.axvline(thresh, color='g', linewidth=4)
        plt.xticks(ticks=np.arange(0, stop=1.1, step=0.1))
        plt.savefig(os.path.join(output_figure_path, 'probability_histogram_{}.png'.format(i)))
        plt.show()


def treshold_predictions(config):
    """
    Treshold all specified samples and store them in a specified directory
    Args:
        config: Thresolding parameters with it's function

    Returns:

    """

    input_dir = config['input_dir']
    output_figure_path = config['output_figure_path']
    segmentation_threshold = config['segmentation_threshold']

    # Get the raw images
    X_path = glob(input_dir)

    print(f'[Threshold predictions] available input to threshold: {len(X_path)}')

    if not os.path.exists(output_figure_path):
        os.makedirs(output_figure_path)
        print('[Threshold predictions] create directory folder:', output_figure_path)

    for i, x_path in enumerate(X_path):
        print(f'[Threshold predictions] read image: {i} / {len(X_path) - 1}  from path: {x_path}')

        # Read the image
        image = io.imread(x_path)

        # Threshold the image
        image_binary = img_as_ubyte(image > segmentation_threshold)
        print(
            f"[Threshold predictions] Binary image with values {np.unique(image_binary)} and shape {image_binary.dtype}")

        # Save resulting binary image to specified output directory
        if output_figure_path:
            save_file_name = Path(output_figure_path) / os.path.basename(x_path)
            print(f"[Threshold predictions] Saving prediction result to {str(save_file_name)}")
            if str(x_path).endswith(".tif") or str(x_path).endswith(".tiff"):
                tifffile.imsave(save_file_name, image_binary)
            else:
                io.imsave(save_file_name, image_binary)


def evaluate_samples(config):
    """
    Evaluate all specified binary predictions and evalute them with the labels
    Args:
        config: Evaluation parameters with it's function

    Returns: dataframe with sample-name, True-Positives, True-Negatives, False-Positives, False-Negatives

    """

    input_dir = config['input_dir']
    label_dir = config['label_dir']
    output_figure_path = config['output_figure_path']
    raw_label_image_criteria = config['raw_label_image_matching_criteria']

    # Get the raw images
    X_path = glob(input_dir)

    # Get the label images
    Y_path = glob(label_dir)

    print(f'[Evaluate] match [ input / label images ] by matching-criteria: {raw_label_image_criteria}')

    print(f'[Evaluate] available [ input / label images ] to evaluate: [ {len(X_path)} / {len(Y_path)} ]')

    if not os.path.exists(output_figure_path):
        os.makedirs(output_figure_path)
        print('[Evaluate] create directory folder:', output_figure_path)

    result_list = []

    for i, x_path in enumerate(tqdm(X_path, total=len(X_path))):
        print(f'[Evaluate] read prediction image: {i} / {len(X_path) - 1}  from path: {x_path}')

        # Find the corresponding matching label image with most overlaps in its filenames
        x_path_filename = os.path.basename(x_path)

        x_path_pattern = \
        [x_path_filename.split(elem)[0] for elem in raw_label_image_criteria if elem in x_path_filename][0]
        y_path_list = [y_path_tmp for y_path_tmp in Y_path if x_path_pattern in y_path_tmp]

        if len(y_path_list) == 0:
            print('[Evaluate] CAUTION continue - could not appropriate label image')
            continue
        else:
            y_path = y_path_list[0]

        print(f'[Evaluate] read label image from path: {y_path}')

        # Read the pred image
        image_pred = io.imread(x_path)

        # Read the true image
        try:
            image_true = io.imread(y_path)
        except:
            image_true = tifffile.imread(y_path)

        # check that the both images should have the same shape
        print(f'[Evaluate] labeled image shape: {image_true.shape} - prediction image shape: {image_pred.shape}')
        assert image_true.shape[:2] == image_pred.shape[:2], "true and prediction image should have the same shape"

        # if label image is not a binary image -> binarize it
        num_unique_true_values = len(np.unique(image_true))

        # if num_unique_true_values != 2: TODO: das hier löschen wenn binärbilder vorhanden
        #     print(f'[Evaluate] binarize label image because of non-binary unique intensities: {num_unique_true_values}')
        #     image_true = utils.binarizeAnnotation(image_true, use_otsu=True, convertToGray=True)

        # transfer all values to {0, 1}
        image_true = img_as_float32(image_true / np.max(image_true))
        image_pred = img_as_float32(image_pred / np.max(image_pred))

        # prepare arrays by flatten these
        y_true = image_true.flatten()
        y_pred = image_pred.flatten()

        # print(f'unique-values of img_true: {np.unique(img_true)} and img_pred: {np.unique(img_pred)}')
        # print(f'img_true-shape: {img_true.shape} ; img_pred-shape: {img_pred.shape}\t\timg_true-type {img_true.dtype} ; img_pred-type: {img_pred.dtype}')
        # print(f'number of values for y_true = {y_true.shape} and {y_pred.shape}\t\t #-y_true == #-y_pred : {y_pred.shape == y_true.shape}')

        # calculate the scores precision, recall, F-1 and separated accuracy # TODO: später löschen!
        acc_score = metrics.accuracy_score(y_true, y_pred)
        prec_score, rec_score, F1_score, support_score = metrics.precision_recall_fscore_support(y_true, y_pred,
                                                                                                 average='binary')
        print(f'tmp acc_score, prec_score, rec_score, F1_score, support_score', acc_score, prec_score, rec_score,
              F1_score, support_score)
        # calculate the scores precision, recall, F-1 and separated accuracy # TODO: später löschen!

        # calculate the True-Positive, False-Positive, True-Negative and False-Negative
        tn, fp, fn, tp = metrics.confusion_matrix(y_true, y_pred).ravel()

        # extract the sample name
        sample_name, ext = os.path.splitext(x_path_filename)

        result_list.append([sample_name, tp, tn, fp, fn])

        # create intermediate dataframe to store a single true/ prediction comparison
        df_results = pd.DataFrame(result_list,
                                  columns=['sample', 'TruePositive', 'TrueNegative', 'FalsePositive', 'FalseNegative'])

        save_path_intermediate = os.path.join(output_figure_path, 'evaluation_intermediate_results.csv')
        df_results.to_csv(save_path_intermediate)
        print(f'[Evaluate] save intermediate results to: {save_path_intermediate}')

    # create dataframe to store the final predictions process per sample
    df_results = pd.DataFrame(result_list,
                              columns=['sample', 'TruePositive', 'TrueNegative', 'FalsePositive', 'FalseNegative'])

    save_path = os.path.join(output_figure_path, 'evaluation_results.csv')
    df_results.to_csv(save_path)
    print(f'[Evaluate] save final results to: {save_path}')

    return df_results


# def evaluate_sample(model, X, Y):  # #X_Y_path):

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

# print('\n\tX:', len(X), 'Y:', len(Y))
#
# # tranfer to numpy required arrays
# # x_test, y_test = np.array(X_test_images), np.array(Y_test_images)
# x_test, y_test = np.array(X), np.array(Y)
# y_test = np.expand_dims(np.array(y_test), axis=-1)
#
# print('\n\ttest data:', x_test.shape, y_test.shape, '\n\n')
#
# scores = model.evaluate(x_test, y_test, verbose=1)
#
# print(f'Score: {model.metrics_names[0]} of {scores[0]} ;\t {model.metrics_names[1]} of {scores[1]}')
#
# return scores