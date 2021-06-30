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
from glob import glob
from pathlib import Path
import matplotlib.pyplot as plt
import tifffile
from skimage import io, filters, img_as_float32, img_as_ubyte
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
    plt.plot(history.history[model.metrics_names[0]], linewidth=3)
    plt.plot(history.history[f'val_{model.metrics_names[0]}'], linewidth=3)
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
        plt.plot(history.history[model.metrics_names[1]], linewidth=3)
        plt.plot(history.history[f'val_{model.metrics_names[1]}'], linewidth=3)
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
    Evaluate all specified samples and store them in a specified directory
    Args:
        config: Evaluation parameters with it's function

    Returns:

    """

    input_dir = config['input_dir']
    label_dir = config['label_dir']
    output_figure_path = config['output_figure_path']
    segmentation_threshold = config['segmentation_threshold']

    # Get the raw images
    X_path = glob(input_dir)

    # Get the label images
    Y_path = glob(label_dir)

    print(f'[Evaluate samples] available input / label images to evaluate: {len(X_path)} / {len(Y_path)}')

    if not os.path.exists(output_figure_path):
        os.makedirs(output_figure_path)
        print('[Evaluate samples] create directory folder:', output_figure_path)

    for i, x_path in enumerate(X_path):
        print(f'[Evaluate samples] read image: {i} / {len(X_path)-1}  from path: {x_path}')

        # Read the image
        image = io.imread(x_path)

        # Threshold the image
        image_binary = img_as_ubyte(image > segmentation_threshold)

        # Save resulting binary image to specified output directory
        if output_figure_path:
            save_file_name = Path(output_figure_path) / os.path.basename(x_path)
            print(f"[Evaluate samples] Saving prediction result to {str(save_file_name)}")
            if str(x_path).endswith(".tif") or str(x_path).endswith(".tiff"):
                tifffile.imsave(save_file_name, image_binary)
            else:
                io.imsave(save_file_name, image_binary)

 # df_scores_all = pd.DataFrame(
    #     columns=['Accuracy', 'Precision', 'Recall', 'F-1', 'TruePositive', 'FalsePositive', 'FalseNegative',
    #              'TrueNegative', 'dice-loss', 'entropy-loss'])

            # if type(abbrevation) == np.float and math.isnan(abbrevation):
            #     print('CAUTION: continue - could not find abbreviaton:', abbrevation)
            #     continue

            # (3) load the original image
            # orig_path = [x for x in path_all_files_origin if abbrevation in x][0]
            # true_path = [x for x in path_all_files_true if abbrevation in x][0]

            # try:
            #     img_orig = tif.imread(orig_path)
            # except:
            #     img_orig = cv2.imread(orig_path)
            # print('\norig_path:\t', orig_path.split('/')[-2:], 'orig-shape:\t', img_orig.shape)
            # try:
            #     img_true = tif.imread(true_path, )
            # except:
            #     img_true = cv2.imread(true_path, 0)

                # if label image is not a binary image: binarize it:
            # if len(np.unique(img_true)) != 2:
            #     print('BEFORE: binarize label image - number of different intensities:', len(np.unique(img_true)))
            #     img_true = binarizeAnnotation(img_true, use_otsu=True, convertToGray=True)
            #     print('AFTER: binarize label image - number of different intensities:', len(np.unique(img_true)))

            # print('true_path:\t', true_path.split('/')[-2:], 'orig-shape:\t', img_true.shape, img_true.dtype)
            # print('true_path:\t', true_path.split('/')[-2:])

            # plt.imshow(img_true)
            # plt.show()

            # (4) on-the-fly prediction with the corresponding model and then evaluated
            # img_pred = model.predict_sample(img_origin=img_orig, savePath=None, save_probMap_image=0, verbose=0)
            # print(
            #     f'img_pred-shape: {img_pred.shape}\timg_pred.min(): {img_pred.min()}\timg_pred.max(): {img_pred.max()}')

            # # (5) save thresholded image
            # path_img_thresh = os.path.join('/asbdata/Philipp/FungIdent/image_data/cross_validation/',
            #                                path_k_models[idx].split('/')[2], path_k_models[idx].split('/')[3],
            #                                path_k_models[idx].split('/')[4])
            # if not os.path.exists(path_img_thresh):
            #     os.makedirs(path_img_thresh)
            #     print('\tcreate mod_dir:', path_img_thresh)

    #         path_img_thresh = os.path.join(path_img_thresh, orig_path.split('/')[-1])
    #         # tif.imsave(path_img_thresh, img_pred)
    #         cv2.imwrite(path_img_thresh, img_pred)
    #         print('\tsave img_pred to:\t', path_img_thresh.split('/')[-5:])
    #
    #         # (6) evaluate the predicted sample with the specified annotation image
    #         # df_scores_single = perform_single_evaluation(true_path, '', verbose=1, img_pred=img_pred)
    #         df_scores_single = perform_single_evaluation(img_true, '', verbose=1, img_pred=img_pred,
    #                                                      img_true_path=true_path)
    #
    #         # (7) append the scores with a collections of all scores
    #         df_scores_all = df_scores_all.append(df_scores_single)
    #
    #         # store the dataframe with all evaluations after each iteration
    #         path_df_single = os.path.join(path_models, 'eval_model_{0}_{1}.csv'.format(idx, abbrevation))
    #         df_scores_all.to_csv(path_df_single)
    #         print('\tsave df_single to:\t', path_df_single)
    #
    # print('\nFinish the whole evaluation with the following scores:\n\n', df_scores_all)
    # return df_scores_all



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



################################################################
# determine an evaluation for given samples (with binary data) #
################################################################

def perform_collected_evaluation(path_models, path_all_files_origin, path_all_files_true, verbose=1):
    df_k_fold_abbrevations = pd.read_csv(os.path.join(path_models, 'k_fold_abbrevations.csv'), index_col=0)

    print('test-data-table:\n', df_k_fold_abbrevations)

    path_k_models = np.sort([x for x in glob(os.path.join(path_models, "*/*")) if os.path.isdir(x)])
    print(f'\n{len(path_k_models)} available models:\n', path_k_models)

    df_scores_all = pd.DataFrame(
        columns=['Accuracy', 'Precision', 'Recall', 'F-1', 'TruePositive', 'FalsePositive', 'FalseNegative',
                 'TrueNegative', 'dice-loss', 'entropy-loss'])

    # iterate about all test-data-k-folds per model
    for idx, row in df_k_fold_abbrevations.iterrows():

        # (0) check if there are enough models for each test-data-permutation
        if idx + 1 > len(path_k_models):
            print(
                f'\n\tABORT EVALUATION: number of iteration ({idx + 1}) > number of available models ({len(path_k_models)})')
            break

        # (1) load the corresponding model and config file to the test data set
        path_single_model = glob(os.path.join(path_k_models[idx], '*.hdf5'))[0]
        print(f'\nmodel-id: {idx}\tmodel-path: {path_single_model}')

        path_single_config = glob(os.path.join(path_k_models[idx], 'mdl_hyperparameter.json'))[0]
        print(f'model-id: {idx}\tconfig-path: {path_single_config}')

        # (2) build the model
        model = SegNet.MySegNet(modelName='FatTissueSegmentation',
                                create_new_model=path_single_model,
                                config_path=path_single_config,
                                verbose=0)

        # iterate about each single test sample
        for abbrevation in row:

            if type(abbrevation) == np.float and math.isnan(abbrevation):
                continue

            # (3) load the original image
            orig_path = [x for x in path_all_files_origin if abbrevation in x][0]
            true_path = [x for x in path_all_files_true if abbrevation in x][0]

            try:
                img_orig = tif.imread(orig_path)
            except:
                img_orig = cv2.imread(orig_path)
            print('\norig_path:\t', orig_path.split('/')[-2:], 'orig-shape:\t', img_orig.shape)
            print('true_path:\t', true_path.split('/')[-2:])

            # (4) on-the-fly prediction with the corresponding model and then evaluated
            img_pred = model.predict_sample(img_origin=img_orig, savePath=None, save_probMap_image=0, verbose=0)
            print(
                f'img_pred-shape: {img_pred.shape}\timg_pred.min(): {img_pred.min()}\timg_pred.max(): {img_pred.max()}')

            # (5) save thresholded image
            path_img_thresh = os.path.join('/asbdata/Philipp/FattyTissueProportion/prediction/',
                                           path_k_models[idx].split('/')[2], path_k_models[idx].split('/')[3],
                                           path_k_models[idx].split('/')[4])
            if not os.path.exists(path_img_thresh):
                os.makedirs(path_img_thresh)
                print('\tcreate mod_dir:', path_img_thresh)

            path_img_thresh = os.path.join(path_img_thresh, orig_path.split('/')[-1])
            tif.imsave(path_img_thresh, img_pred)
            print('\tsave img_pred to:\t', path_img_thresh.split('/')[-5:])

            # (6) evaluate the predicted sample with the specified annotation image
            df_scores_single = perform_single_evaluation(true_path, '', verbose=1, img_pred=img_pred)

            # (7) append the scores with a collections of all scores
            df_scores_all = df_scores_all.append(df_scores_single)

            # store the dataframe with all evaluations after each iteration
            path_df_single = os.path.join(path_models, 'eval_model_{0}_{1}.csv'.format(idx, abbrevation))
            df_scores_all.to_csv(path_df_single)
            print('\tsave df_single to:\t', path_df_single)

    print('\nFinish the whole evaluation with the following scores:\n\n', df_scores_all)
    return df_scores_all


################################################################
# determine an evaluation for given samples (with binary data) #
################################################################

def perform_multiple_evaluation(all_files_original, all_files_annotation, all_files_prediction, path_save_directory):
    df_scores_all = pd.DataFrame(
        columns=['Accuracy', 'Precision', 'Recall', 'F-1', 'TruePositive', 'FalsePositive', 'FalseNegative',
                 'TrueNegative', 'dice-loss', 'entropy-loss'])

    # iterate over all predictions
    for i, tmp_pred_path in enumerate(all_files_prediction):
        # choose sample: remove the ending '_prediction_threshold.png' (last 25 chars) and the first 6 chars
        abbrevation = tmp_pred_path.split('/')[-1][:-25][:8]

        print('\n[{0} / {1}] - abbrevation: {2}'.format(i, len(all_files_prediction) - 1, abbrevation))

        orig_path = [x for x in all_files_original if abbrevation in x][0]
        true_path = [x for x in all_files_annotation if abbrevation in x][0]
        pred_path = [x for x in all_files_prediction if abbrevation in x][0]

        print('\torig_path:\t', orig_path.split('/')[-2:])
        print('\ttrue_path:\t', true_path.split('/')[-2:])
        print('\tpred_path:\t', pred_path.split('/')[-2:])

        df_score_single = perform_single_evaluation(true_path, pred_path, verbose=1)
        df_score_single.to_csv(os.path.join(path_save_directory, 'Metric_evaluation_{0}.csv'.format(i)), sep=',')

        df_scores_all = df_scores_all.append(df_score_single, ignore_index=True)

    df_scores_all.to_csv(os.path.join(path_save_directory, 'Metric_evaluation.csv'), sep=',')


################################################################
# determine an evaluation for given samples (with binary data) #
################################################################

def perform_single_evaluation(img_true_path, img_pred_path, verbose, img_pred=None):
    # load images, either load the prediciton image OR use directly the image for the evaluation
    img_true = cv2.imread(img_true_path, 0)
    if img_pred is None:
        img_pred = cv2.imread(img_pred_path, 0)

    # normalize to [0,1]
    img_true = (img_true / np.max(img_true)).astype(np.uint8)
    img_pred = (img_pred / np.max(img_pred)).astype(np.uint8)

    # prepare arrays by flatten these
    y_pred = img_pred.flatten()
    y_true = img_true.flatten()

    if verbose > 1:
        print(f'unique-values of img_true: {np.unique(img_true)} and img_pred: {np.unique(img_pred)}')
        print(
            f'img_true-shape: {img_true.shape} ; img_pred-shape: {img_pred.shape}\t\timg_true-type {img_true.dtype} ; img_pred-type: {img_pred.dtype}')
        print(
            f'number of values for y_true = {y_true.shape} and {y_pred.shape}\t\t #-y_true == #-y_pred : {y_pred.shape == y_true.shape}')

    # calculate the scores precision, recall, F-1 and separated accuracy
    acc_score = sklearn.metrics.accuracy_score(y_true, y_pred)
    prec_score, rec_score, F1_score, support_score = sklearn.metrics.precision_recall_fscore_support(y_true, y_pred,
                                                                                                     average='binary')

    # calculate the True-Positive, False-Positive, True-Negative and False-Negative 
    tn, fp, fn, tp = sklearn.metrics.confusion_matrix(y_true, y_pred).ravel()

    # calculate the loss and the binary/cross entropy-loss 
    soerensen_dice_loss = dice_loss(y_true, y_pred)
    entropy_loss = entropy_dice_loss(y_true, y_pred)

    if verbose > 0:
        print(
            f'\nAccurary:\t{acc_score}\nPrecision:\t{prec_score}\nRecall:\t\t{rec_score}\nF-1 Measure:\t{F1_score}\nsupport-values:\t{support_score}\nTrue-Positive:\t{tp}\nFalse-Positive:\t{fp}\nFalse-Negative:\t{fn}\nTrue-Negative:\t{tn}\ndice-loss:\t{soerensen_dice_loss}\nentropy-loss:\t{entropy_loss}')

    # create dataframe with all performance metrics    
    df_scores = pd.DataFrame({
        'Accuracy': acc_score,
        'Precision': prec_score,
        'Recall': rec_score,
        'F-1': F1_score,
        'TruePositive': tp,
        'FalsePositive': fp,
        'FalseNegative': fn,
        'TrueNegative': tn,
        'dice-loss': soerensen_dice_loss,
        'entropy-loss': entropy_loss
    }, index=[img_true_path.split('/')[-1].split('.png')[0]])

    # calculate the MCC
    df_scores["MCC"] = MCC(df_scores)

    if verbose > 1:
        TP, FP, TN, FN = determine_standard_measure(y_true, y_pred)
        print('\nCreate manual metrics for control purpose:\n\nTP, FP, TN, FN:', TP, FP, TN, FN)

        acc, P, R, F_1, = determine_advanded_measures(TP, FP, TN, FN)
        print(f'Accurary:\t{acc}\nPrecision:\t{P}\nRecall:\t\t{R}\nF-1 Measure:\t{F_1}')

    # plot the ROC curve 
    if verbose > 1:
        fpr, tpr, thresholds = sklearn.metrics.roc_curve(y_true, y_pred)

        print(f'\nFalse-positive-rate: {fpr}\tTrue-positive-rate: {tpr}\t ROC-thresholds: {thresholds}')
        plt.plot(fpr, tpr)
        plt.title('Receiver operating score')
        plt.show()

    return df_scores


################################################################
#          predict all images wihtin a given directory         #
################################################################

def predict_all_samples(path_origin, path_save_directory, model_path, verbose):
    # (1) read all specified origin images
    all_files_original = glob(path_origin + '/*.*')
    print('\nnumber of images to predict:', len(all_files_original))

    # (2) create save directory
    if not os.path.exists(path_save_directory):
        os.makedirs(path_save_directory)
        print('\ncreate folder:', path_save_directory)

    # (3) build the model
    config_path = glob('/'.join(model_path.split('/')[:-1]) + '/mdl_hyperparameter*')[0]

    model = SegNet.MySegNet(modelName=model_path.split('/')[-4],
                            create_new_model=model_path,
                            config_path=config_path,
                            verbose=0)

    # (4) create folder for figures if specified
    if verbose > 0:
        path_save_figure = os.path.join(path_save_directory, 'figure')
        if not os.path.exists(path_save_figure):
            os.makedirs(path_save_figure)
            print('\ncreate folder:', path_save_figure)

    # (5.1) create list to store whether a prediction process per sample worked fine
    list_process = []

    # (6) iterate through all images
    for idx, path_orig in enumerate(all_files_original):

        # build save path
        prefix = path_orig.split('/')[-1].split('tif')[0][:-1]
        print('\n[ {0} / {1} ] - prefix: {2}'.format(idx, len(all_files_original) - 1, prefix))

        filename = os.path.join(path_save_directory, prefix)
        print('\tsave path:', filename)

        try:

            try:
                img_origin = tif.imread(path_orig)
            except:
                img_origin = cv2.imread(path_orig)
            print('\tpath - original:', path_orig, img_origin.shape, img_origin.dtype)

            img_predict_thresh = model.predict_sample(img_origin=img_origin,
                                                      savePath=filename,
                                                      save_probMap_image=False,
                                                      verbose=1)

            if verbose > 1:
                plt.figure(figsize=(12, 12))
                plt.imshow(img_predict_thresh, cmap='gray')
                plt.title(prefix, fontsize=20)
                plt.grid(False)
                plt.tight_layout()
                plt.savefig(os.path.join(path_save_figure, prefix + '_thresh.png'))
                plt.show()

            list_process.append([prefix, 1])

        except:
            print('\t[FAIL] with image:', path_orig)
            list_process.append([prefix, 0])
            continue

        # (5.2) create intermediate data frame to store whether a prediction process of a single sample worked fine
        df_process_tmp = pd.DataFrame(list_process, columns=['sample', 'worked'])
        df_process_tmp.to_csv(os.path.join(path_save_directory, 'prediction_process_tmp.csv'))

    # (5.3) create data frame to store whether a prediction process per sample worked fine
    df_process = pd.DataFrame(list_process, columns=['sample', 'worked'])

    path_df_process = os.path.join(path_save_directory, 'prediction_process.csv')
    df_process.to_csv(path_df_process)
    print('\tsave df_process to:\t', path_df_process)