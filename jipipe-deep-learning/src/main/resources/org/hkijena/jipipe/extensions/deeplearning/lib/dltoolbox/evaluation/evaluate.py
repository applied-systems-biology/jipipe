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

import os
from glob import glob
import numpy as np
import pandas as pd
import cv2
import matplotlib.pyplot as plt
import sklearn
import skimage
import tifffile as tif
import math
import keras.backend as K

from models import SegNet
import utils

from dltoolbox.models.metrics import *


################################################################
#                    plot history of metrics                   #
################################################################

def plot_history(history, path):
    # Plot training & validation accuracy values
    figure = plt.figure(figsize=(12, 12))
    plt.plot(history.history['acc'])
    plt.plot(history.history['val_acc'])
    plt.title('Model accuracy')
    plt.ylabel('Accuracy')
    plt.xlabel('Epoch')
    plt.legend(['Train', 'Test'], loc='upper left')
    plt.savefig(os.path.join( path , 'training_validation_accuracy.png'))
    plt.show()

    # Plot training & validation loss values
    figure = plt.figure(figsize=(12, 12))
    plt.plot(history.history['loss'])
    plt.plot(history.history['val_loss'])
    plt.title('Model loss')
    plt.ylabel('Loss')
    plt.xlabel('Epoch')
    plt.legend(['Train', 'Test'], loc='upper left')
    plt.savefig(os.path.join( path , 'training_validation_loss.png'))
    plt.show()

    # Plot training & validation AUC
    figure = plt.figure(figsize=(12, 12))
    plt.plot(history.history['auc'])
    plt.plot(history.history['val_auc'])
    plt.title('Model AUC')
    plt.ylabel('True Positive Rate')
    plt.xlabel('Epoch')
    plt.legend(['Train', 'Test'], loc='upper left')
    plt.savefig(os.path.join( path , 'training_validation_auc.png'))
    plt.show()

##############################################################
#           plot all probabilities for given samples         #
##############################################################

def plot_probabilities(model,  img_orig_path, verbose, filter_min_value=None):
    
    # get necessary parameter 
    winW, winH = model.img_shape[0], model.img_shape[1]
    windowStep = model.img_shape[0]
    model_threshold = model.config["segmentation_threshold"][0] 

    # load images
    img_pred = cv2.imread(img_orig_path, 1)

    print('read image:', img_orig_path, 'with shape:', img_pred.shape)
    
    probabilities = []

    # iterate about each window
    for (row, col, window) in utils.sliding_window(img_pred, step_size=windowStep, window_size=(winW, winH)):

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
            window = np.expand_dims( window, axis=0 )
        
        y_pred = model.unet.predict_on_batch( [window] )

        if verbose > 0 and y_pred.max() > 0.85:
            print(f'shape: {y_pred.shape}\tmin: {y_pred.min()}\tmax: {y_pred.max()}\ttype: {y_pred.dtype}')

        y_pred = y_pred.flatten()
        
        probabilities.extend( y_pred )
        
    print(f'number of prability-values: {len(probabilities)}\tmin-value: {np.min(probabilities)}\tmax-value: {np.max(probabilities)}')

    if filter_min_value is not None:
        probabilities = np.array(probabilities)
        probabilities = probabilities[probabilities >= filter_min_value]        
        print(f'number of prability-values: {len(probabilities)}\tmin-value: {probabilities.min()}\tmax-value: {probabilities.max()}')

    # calculate an otsu-threshold
    thresh = skimage.filters.threshold_otsu( np.array(probabilities) )
    print(f'own chosen threshold: {model_threshold}\tVS\totsu-threshold: {thresh}')

    plt.hist( np.array(probabilities) ) #, bins=256)
    plt.title('Probability histogram', fontsize=20)
    plt.axvline(model_threshold, color='r', linewidth=4)
    plt.axvline(thresh, color='g', linewidth=4)
    plt.xticks(ticks=np.arange(0, stop=1.1, step=0.1))
    plt.savefig('probability_histogram.png')
    plt.show()

##############################################################
#           determine standard evalutaion metrics            #
##############################################################

def determine_standard_measure(y_true, y_pred):
    
    TP = sum( y_true == y_pred )
    FP = sum( (y_pred == 1) & (y_true != y_pred) )
    TN = sum( (y_true == y_pred) & (y_true == 0) & (y_pred == 0) )
    FN = sum( (y_pred == 0) & (y_true != y_pred) )
        
    return(TP, FP, TN, FN)

def determine_advanded_measures(TP, FP, TN, FN):

    accuracy = (TP+TN) / (TP+TN+FP+FN)
    precision = TP / ( TP+FP )
    recall = TP / ( TP+FN )
    F_1 = ( 2 * (precision * recall) ) / (precision + recall) 

    return (accuracy, precision, recall, F_1)

# def dice_coeff(y_true, y_pred):
#     smooth = 1.
#     intersection = np.sum(y_true * y_pred)
#     score = (2. * intersection + smooth) / (np.sum(y_true) + np.sum(y_pred) + smooth)
#     return score

# def dice_loss(y_true, y_pred):
#     loss = 1 - dice_coeff(y_true, y_pred)
#     return loss

def entropy_dice_loss(y_true, y_pred):
    # calculate the binary/cross entropy-loss dependent on binary or multinominal data 
    loss = sklearn.metrics.log_loss(y_true, y_pred) + dice_loss(y_true, y_pred)
    return loss

def Jaccard_index(gt_bb, pred_bb):
    '''
    :param gt_bb: ground truth bounding box
    :param pred_bb: predicted bounding box
    '''
    gt_bb = tf.stack([
        gt_bb[:, :, :, :, 0] - gt_bb[:, :, :, :, 2] / 2.0,
        gt_bb[:, :, :, :, 1] - gt_bb[:, :, :, :, 3] / 2.0,
        gt_bb[:, :, :, :, 0] + gt_bb[:, :, :, :, 2] / 2.0,
        gt_bb[:, :, :, :, 1] + gt_bb[:, :, :, :, 3] / 2.0])
    gt_bb = tf.transpose(gt_bb, [1, 2, 3, 4, 0])
    pred_bb = tf.stack([
        pred_bb[:, :, :, :, 0] - pred_bb[:, :, :, :, 2] / 2.0,
        pred_bb[:, :, :, :, 1] - pred_bb[:, :, :, :, 3] / 2.0,
        pred_bb[:, :, :, :, 0] + pred_bb[:, :, :, :, 2] / 2.0,
        pred_bb[:, :, :, :, 1] + pred_bb[:, :, :, :, 3] / 2.0])
    pred_bb = tf.transpose(pred_bb, [1, 2, 3, 4, 0])
    area = tf.maximum( 0.0,
        tf.minimum(gt_bb[:, :, :, :, 2:], pred_bb[:, :, :, :, 2:]) -
        tf.maximum(gt_bb[:, :, :, :, :2], pred_bb[:, :, :, :, :2]))

    intersection_area= area[:, :, :, :, 0] * area[:, :, :, :, 1]
    gt_bb_area = (gt_bb[:, :, :, :, 2] - gt_bb[:, :, :, :, 0]) * \
        (gt_bb[:, :, :, :, 3] - gt_bb[:, :, :, :, 1])
    pred_bb_area = (pred_bb[:, :, :, :, 2] - pred_bb[:, :, :, :, 0]) * \
        (pred_bb[:, :, :, :, 3] - pred_bb[:, :, :, :, 1])
    union_area = tf.maximum(gt_bb_area + pred_bb_area - intersection_area,1e-10)
    iou = tf.clip_by_value(intersection_area / union_area, 0.0, 1.0)

    return iou

# def MCC(df_tmp):
#     '''
#     calculate the Matthew correlation coefficient
#     '''
    
#     TP = df_tmp["TruePositive"]
#     FP = df_tmp["FalsePositive"]
#     FN = df_tmp["FalseNegative"]
#     TN = df_tmp["TrueNegative"]
    
#     counter = (TN * TP) - (FP * FN)
#     denominator = np.sqrt( (TN + FN)*(FP + TP)*(TN + FP)*(FN + TP) )
    
#     return counter / denominator

################################################################
# determine an evaluation for given samples (with binary data) #
################################################################

def perform_collected_evaluation(path_models, path_all_files_origin, path_all_files_true, verbose=1):

    df_k_fold_abbrevations = pd.read_csv(os.path.join(path_models, 'k_fold_abbrevations.csv') , index_col=0)
    
    print('test-data-table:\n', df_k_fold_abbrevations)
    
    path_k_models = np.sort([ x for x in glob(os.path.join(path_models, "*/*")) if os.path.isdir(x) ])
    print(f'\n{len(path_k_models)} available models:\n', path_k_models)
    
    df_scores_all = pd.DataFrame(columns=['Accuracy', 'Precision', 'Recall', 'F-1', 'TruePositive', 'FalsePositive', 'FalseNegative', 'TrueNegative', 'dice-loss', 'entropy-loss'])
    
    # iterate about all test-data-k-folds per model
    for idx, row in df_k_fold_abbrevations.iterrows():
        
        # (0) check if there are enough models for each test-data-permutation
        if idx+1 > len(path_k_models):
            print(f'\n\tABORT EVALUATION: number of iteration ({idx+1}) > number of available models ({len(path_k_models)})')
            break
        
        # (1) load the corresponding model and config file to the test data set
        path_single_model = glob(os.path.join(path_k_models[ idx ], '*.hdf5'))[0]
        print(f'\nmodel-id: {idx}\tmodel-path: {path_single_model}')

        path_single_config = glob(os.path.join(path_k_models[ idx ], 'mdl_hyperparameter.json'))[0]
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
            print(f'img_pred-shape: {img_pred.shape}\timg_pred.min(): {img_pred.min()}\timg_pred.max(): {img_pred.max()}')            

            # (5) save thresholded image
            path_img_thresh = os.path.join('/asbdata/Philipp/FattyTissueProportion/prediction/', 
                    path_k_models[idx].split('/')[2], path_k_models[idx].split('/')[3], path_k_models[idx].split('/')[4])
            if not os.path.exists(path_img_thresh):
                os.makedirs(path_img_thresh)
                print('\tcreate mod_dir:', path_img_thresh)
            
            path_img_thresh = os.path.join(path_img_thresh, orig_path.split('/')[-1])
            tif.imsave(path_img_thresh, img_pred)            
            print('\tsave img_pred to:\t', path_img_thresh.split('/')[-5:])
            
            # (6) evaluate the predicted sample with the specified annotation image
            df_scores_single = perform_single_evaluation(true_path, '', verbose=1, img_pred=img_pred)
                        
            # (7) append the scores with a collections of all scores
            df_scores_all = df_scores_all.append( df_scores_single )
           
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

    df_scores_all = pd.DataFrame(columns=['Accuracy', 'Precision', 'Recall', 'F-1', 'TruePositive', 'FalsePositive', 'FalseNegative', 'TrueNegative', 'dice-loss', 'entropy-loss'])

    # iterate over all predictions
    for i, tmp_pred_path in enumerate(all_files_prediction):

        # choose sample: remove the ending '_prediction_threshold.png' (last 25 chars) and the first 6 chars
        abbrevation = tmp_pred_path.split('/')[-1][:-25][:8]

        print('\n[{0} / {1}] - abbrevation: {2}'.format(i, len(all_files_prediction)-1, abbrevation))
        
        orig_path = [x for x in all_files_original if abbrevation in x][0]
        true_path = [x for x in all_files_annotation if abbrevation in x][0]
        pred_path = [x for x in all_files_prediction if abbrevation in x][0]
        
        print('\torig_path:\t', orig_path.split('/')[-2:])
        print('\ttrue_path:\t', true_path.split('/')[-2:])
        print('\tpred_path:\t', pred_path.split('/')[-2:])
        
        df_score_single = perform_single_evaluation(true_path, pred_path, verbose=1)
        df_score_single.to_csv( os.path.join(path_save_directory, 'Metric_evaluation_{0}.csv'.format(i) ), sep=',')
        
        df_scores_all = df_scores_all.append(df_score_single, ignore_index=True) 
        
    df_scores_all.to_csv( os.path.join(path_save_directory, 'Metric_evaluation.csv' ), sep=',')
    
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
        print(f'img_true-shape: {img_true.shape} ; img_pred-shape: {img_pred.shape}\t\timg_true-type {img_true.dtype} ; img_pred-type: {img_pred.dtype}')
        print(f'number of values for y_true = {y_true.shape} and {y_pred.shape}\t\t #-y_true == #-y_pred : {y_pred.shape == y_true.shape}')
    
    # calculate the scores precision, recall, F-1 and separated accuracy
    acc_score = sklearn.metrics.accuracy_score(y_true, y_pred)
    prec_score, rec_score, F1_score, support_score = sklearn.metrics.precision_recall_fscore_support(y_true, y_pred, average='binary')

    # calculate the True-Positive, False-Positive, True-Negative and False-Negative 
    tn, fp, fn, tp = sklearn.metrics.confusion_matrix(y_true, y_pred).ravel()
    
    # calculate the loss and the binary/cross entropy-loss 
    soerensen_dice_loss = dice_loss(y_true, y_pred)    
    entropy_loss = entropy_dice_loss(y_true, y_pred)
    
    if verbose > 0:
        print(f'\nAccurary:\t{acc_score}\nPrecision:\t{prec_score}\nRecall:\t\t{rec_score}\nF-1 Measure:\t{F1_score}\nsupport-values:\t{support_score}\nTrue-Positive:\t{tp}\nFalse-Positive:\t{fp}\nFalse-Negative:\t{fn}\nTrue-Negative:\t{tn}\ndice-loss:\t{soerensen_dice_loss}\nentropy-loss:\t{entropy_loss}')

    # create dataframe with all performance metrics    
    df_scores = pd.DataFrame( {
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
    }, index=[ img_true_path.split('/')[-1].split('.png')[0] ] )

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
    all_files_original = glob(path_origin+'/*.*')
    print('\nnumber of images to predict:', len(all_files_original))    
    
    # (2) create save directory
    if not os.path.exists(path_save_directory):
        os.makedirs(path_save_directory)
        print('\ncreate folder:', path_save_directory)

    # (3) build the model
    config_path = glob( '/'.join(model_path.split('/')[:-1])+'/mdl_hyperparameter*' )[0]

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
        print('\n[ {0} / {1} ] - prefix: {2}'.format(idx, len(all_files_original)-1, prefix))
            
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
                plt.figure(figsize=(12,12))
                plt.imshow(img_predict_thresh, cmap='gray')
                plt.title(prefix, fontsize=20)
                plt.grid(False)
                plt.tight_layout()
                plt.savefig( os.path.join(path_save_figure, prefix+'_thresh.png') )
                plt.show()

            list_process.append([ prefix , 1 ])

        except:
            print('\t[FAIL] with image:', path_orig)
            list_process.append([ prefix , 0 ])
            continue

        # (5.2) create intermediate data frame to store whether a prediction process of a single sample worked fine
        df_process_tmp = pd.DataFrame(list_process, columns=['sample', 'worked'])
        df_process_tmp.to_csv( os.path.join(path_save_directory, 'prediction_process_tmp.csv') )

    # (5.3) create data frame to store whether a prediction process per sample worked fine
    df_process = pd.DataFrame(list_process, columns=['sample', 'worked'])

    path_df_process = os.path.join(path_save_directory, 'prediction_process.csv')
    df_process.to_csv( path_df_process )
    print('\tsave df_process to:\t', path_df_process)




# def evaluate_model(model, X, Y):  # #X_Y_path):
#
#     # read ALL images path
#     # original_path_all = np.sort( glob( os.path.join( input_path, 'original', '*.png') ) )
#     # labels_path_all = np.sort( glob( os.path.join( input_path, 'labels', '*.png') ) )
#     # print('[INFO] total images in directory - originals: {0} and labels: {1}'.format(len(original_path_all), len(labels_path_all)) )
#
#     # X_test_path, Y_test_path = X_Y_path
#
#     # print('\n\tX_test_path:', len(X_test_path), 'Y_test_path:', len(Y_test_path))
#
#     # read all test images
#     # X_test_images = skimage.io.ImageCollection(X_test_path)
#     # Y_test_images = skimage.io.ImageCollection(Y_test_path)
#     # print('\n\tX_test_images:', len(X_test_images), 'Y_test_images:', len(Y_test_images) )
#
#     print('\n\tX:', len(X), 'Y:', len(Y))
#
#     # tranfer to numpy required arrays
#     # x_test, y_test = np.array(X_test_images), np.array(Y_test_images)
#     x_test, y_test = np.array(X), np.array(Y)
#     y_test = np.expand_dims(np.array(y_test), axis=-1)
#
#     print('\n\ttest data:', x_test.shape, y_test.shape, '\n\n')
#
#     scores = model.evaluate(x_test, y_test, verbose=1)
#
#     print(f'Score: {model.metrics_names[0]} of {scores[0]} ;\t {model.metrics_names[1]} of {scores[1]}')
#
#     return scores