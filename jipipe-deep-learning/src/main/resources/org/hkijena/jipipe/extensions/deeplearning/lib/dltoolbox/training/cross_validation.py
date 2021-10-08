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
import pandas as pd
import json
import matplotlib.pyplot as plt
from glob import glob
import skimage

from models import SegNet


#############################################################
#                 K-fold-cross-validation                   #
#############################################################

def cross_validation(model_config, config,  
 modelName, config_path, retrain_one_model, verbose):
    """
    Perform a cross-validation.

    Args:
        modelName ([type]): [description]
        config_path ([type]): [description]
        retrain_one_model (bool): true if a new model will be created after each k-fold
        verbose ([type]): [description]
    """

    # assign hyper-parameter for training procedure
    # input_dir = config['input_dir']
    # label_dir = config['label_dir']
    # n_epochs = config['max_epochs']
    # batch_size = config['batch_size']
    # validation_split = config['validation_split']
    # monitor_loss = config['monitor_loss']
    # input_model_path = config["input_model_path"] if "input_model_path" in config else model_config['output_model_path']
    # output_model_path = config['output_model_path']
    # output_model_json_path = config['output_model_json_path']
    
    num_K_folds = config['num_K_folds']
    # csv-file where information about the images are stored with samples and their corresponding abbreviation
    k_fold_info_path = config['k_fold_info_path']
    output_k_fold_model_info = config['output_k_fold_model_info']

    ### prepare the test-data for the cross-validation
    df_data_info = pd.read_csv( k_fold_info_path )
    abbrevations = np.array( list(df_data_info['abbrevation']) )
    # randomly shuffle samples for the test data and split them accordingly to K-fold
    np.random.shuffle( abbrevations )
    k_fold_abbrevations = np.array_split( abbrevations, indices_or_sections=num_K_folds)

    print('\n\tk_fold_info:\n', df_data_info)
    print('\n\tabbrevations:\n', abbrevations)
        
    print('\n\t[INFO] num_K_folds:', num_K_folds, '\t\tk_fold_abbrevations:\n', k_fold_abbrevations)

    ### create folder of modelname and store a table with the test data per model within a cross-fold
    #mod_dir = os.path.join(config["checkpoint_dir"][0], 'models', modelName)
    
    # if not os.path.exists(mod_dir):
    #     os.makedirs(mod_dir)
    #     print('\n\tcreate mod_dir:', mod_dir)
    
    df_k_fold_abbrevations = pd.DataFrame(k_fold_abbrevations)

    #path_k_fold_abbrevations = os.path.join(mod_dir, 'k_fold_abbrevations.csv')
    #df_k_fold_abbrevations.to_csv(path_k_fold_abbrevations, sep=',')
    df_k_fold_abbrevations.to_csv(output_k_fold_model_info, sep=',')

    return    

    if verbose > 0:
        print('\tstore table with infos about test-data per model to:', path_k_fold_abbrevations, '\n')


    input_path = config['input_dir'][0]
    ### read ALL images path    
    original_path_all = np.sort( glob( os.path.join(input_path, 'original', '*.png') ) ) 
    labels_path_all = np.sort( glob( os.path.join(input_path, 'labels', '*.png') ) )
    # original_path, labels_path = utils.calculateLabelFraction(config['input_dir'][0], target_fraction=0.999)
    print('\n\t[INFO] total images in directory - originals: {0} and labels: {1}'.format(len(original_path_all), len(labels_path_all)) )

    assert len(original_path_all) == len(labels_path_all) > 0

    ### Define per-fold score containers and DataFrame
    metric_per_fold = []
    loss_per_fold = []
    
    ##### repeating cross-fold-validation where one model (SegNet) will be used for all folds
    if config['model'][0] == "SegNet" and retrain_one_model:
        
        model = SegNet.MySegNet(modelName=modelName, 
                    create_new_model=[], 
                    config_path=config_path, 
                    verbose=1)

    ##### perform K-fold-cross-validation #####
    for nr, k_fold_abbrevation in enumerate(k_fold_abbrevations):
        print('\n\n\t--->k-fold-validation: [ {0} / {1} ]\t abbrevations for testing: {2}'.format(nr, num_K_folds, k_fold_abbrevation))
        
        ### read images for the current cross-fold validation from directory ###        
        X_trainValid_path, X_test_path = [], [] 
        Y_trainValid_path, Y_test_path = [], []
        
        # collect all test paths
        test_path = [ (orig_path, label_path) for orig_path, label_path in zip(original_path_all, labels_path_all) for abbrev in k_fold_abbrevation if abbrev in orig_path and abbrev in label_path ]
        for orig_path, label_path in test_path:
            X_test_path.append( orig_path )
            Y_test_path.append( label_path )

        # collect all train-validation paths
        for orig_path, label_path in zip(original_path_all, labels_path_all):
            if orig_path not in X_test_path:
                X_trainValid_path.append( orig_path )
            if label_path not in Y_test_path:
                Y_trainValid_path.append( label_path )
    
        if verbose > 0:
            print('\n\tX_trainValid_path:', len(X_trainValid_path), 'X_test_path:', len(X_test_path), 'sum:', len(X_trainValid_path) + len(X_test_path))
            print('\tY_trainValid_path:', len(Y_trainValid_path), 'Y_test_path:', len(Y_test_path), 'sum:', len(Y_trainValid_path) + len(Y_test_path))
                
        cross_validation_data = ( X_trainValid_path, Y_trainValid_path )

        ##### distinguish for model-architecture and create new model create_new_model=empty list OR use one model 
        if config['model'][0] == "SegNet" and not retrain_one_model:

            ##### non-repeating cross-fold-validation where for each fold a new model will be created    
            model = SegNet.MySegNet(modelName=modelName, 
                            create_new_model=[], 
                            config_path=config_path, 
                            verbose=1)

        ### train SetNet with the corresponding training AND validation data (without test data)        
        model.train_model(verbose=1, perform_cross_validation=cross_validation_data)

        print('\n-------> Test-scenario <-------')
        
        ### evaluate the test data and store the results in the corresponding model-folder

        test_data = ( X_test_path, Y_test_path )

        scores = model.evaluate_model(X_Y_path=test_data)
        
        metric_per_fold.append( scores[1] )
        loss_per_fold.append( scores[0] )

        df_scores = pd.DataFrame( {
            model.unet.metrics_names[0]: scores[0],
            model.unet.metrics_names[1]: scores[1]
        }, index=[ '_'.join(k_fold_abbrevation) ] )                        
        df_scores.to_csv( os.path.join( model.mod_dir, 'Test_evaluation_nr_{0}.csv'.format(nr) ), sep=',')    


    #####  Provide average scores for K-fold-cross-validation
    print('\n------------------------------------------------------------------------\n')
    print('Score per fold:')
    for i in range(0, len(metric_per_fold)):
        print('------------------------------------------------------------------------')
        print(f'> Fold {i+1} - Loss: {loss_per_fold[i]} - Metric-1: {metric_per_fold[i]}%')
    
    print('\nAverage scores for all folds:')
    print(f'> Metric-1: {np.mean(metric_per_fold)} (+- {np.std(metric_per_fold)})')
    print(f'> Loss: {np.mean(loss_per_fold)}')

    df = pd.DataFrame({'loss_per_fold': loss_per_fold, 'metric_per_fold': metric_per_fold})
    df.to_csv(os.path.join(config["checkpoint_dir"][0], 'models', modelName, 'Test_evaluation_total_{0}.csv'.format(modelName) ) , sep=',')    
    print('store evaluation results to: Test_evaluation_{0}.csv'.format(modelName))

################################################################
# manual-cross-validation (performed on pre-defined test data) #
################################################################

def manual_cross_validation(modelName, config_path, test_data_path, verbose):
    
    ### read parameter from json file
    with open(config_path) as json_file:
        config = json.load(json_file)

    if verbose > 0:
        [ print(parameter,':\t', value_desc) for parameter, value_desc in config.items() ]