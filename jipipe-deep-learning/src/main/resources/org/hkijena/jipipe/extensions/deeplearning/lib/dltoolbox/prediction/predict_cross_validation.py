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

Script to predict a networks within a cross-validation
"""

import os
import numpy as np
import pandas as pd
from glob import glob
from tqdm import tqdm
import json

from dltoolbox.utils import load_and_compile_model


# import time
# from datetime import datetime
# import cv2
# import skimage
# from scipy import ndimage
# import tifffile as tif
# import matplotlib.pyplot as plt
# import matplotlib as mpl
# import matplotlib.image as mpimg


def predict_cross_validation(model_config, config, model=None):
    """
    Predicts multiple models as a cross-validation with a corresponding information table
    Args:
        model_config: the model config
        config: the config for this prediction
        model: An existing model (optional)

    Returns: Prediction results (list of predictions, list of input files)

    """

    # (path_models, path_all_files_origin, path_all_files_true):
    input_dir = config['input_dir']
    output_dir = config['output_dir']
    info_table_dir = config['info_table_dir']
    models_path = config['models_path']

    # read in the data information table
    df_k_fold_abbrevations = pd.read_csv(info_table_dir, index_col=0)

    # get all paths for the input images
    input_images = glob(input_dir)
    print('[Predict.cross-validation] Total available images:', len(input_images))
    print('[Predict.cross-validation] Images to predict:', df_k_fold_abbrevations[['0', '1', '2']].count(axis=1).sum())

    # get all paths for the models
    available_model_paths = glob(models_path)

    df_k_fold_abbrevations['model_path'] = None
    for idx, row in df_k_fold_abbrevations.iterrows():
        # get the match of the model_name within all parent directories of available model paths
        single_model_path = [elem for elem in available_model_paths if str(row['model_name']) in elem]

        df_k_fold_abbrevations.loc[idx, 'model_path'] = single_model_path

    print('[Predict.cross-validation] info_table:\n', df_k_fold_abbrevations)

    for idx, row in tqdm(df_k_fold_abbrevations.iterrows(), total=df_k_fold_abbrevations.shape[0]):

        print(f"\n[Predict.cross-validation] model_id - # models: [ {idx} / {df_k_fold_abbrevations.shape[0]} ]")

        # (1) load the corresponding model and config file for the test images
        for (dirpath, dirnames, filenames) in os.walk(row['model_path']):
            model_files_path = [os.path.join(dirpath, file) for file in filenames]

        input_model_path = [elem for elem in model_files_path if str('model.hdf5') in elem][0]
        input_model_json_path = [elem for elem in model_files_path if str('model.json') in elem][0]
        model_config_path = [elem for elem in model_files_path if str('model-config.json') in elem][0]

        with open(model_config_path, "r") as f:
            model_config = json.load(f)

        print(f"[Predict.cross-validation] model-path: {single_model_path}")

        # (2) load and build the model
        #model = load_and_compile_model(model_config, input_model_path, model)

        # (3) iterate about each single test sample
        for abbreviation in row[['0', '1', '2']]:

            if type(abbreviation) == np.float and math.isnan(abbreviation):
                print(f"[Predict.cross-validation] CAUTION: continue - could not find abbreviation: {abbreviation}")
                continue

            # (4) get the original image path
            orig_path = [x for x in path_all_files_origin if abbreviation in x][0]
            print(f"[Predict.cross-validation] original image path: {orig_path}")

            # (5) read the image
            try:
                img_orig = tif.imread(orig_path)
            except:
                img_orig = cv2.imread(orig_path)

            # TODO: hier gehts weiter
            # (6) on-the-fly prediction with the corresponding model and then evaluated
            img_pred = model.predict_sample(img_origin=img_orig, savePath=None, save_probMap_image=0, verbose=0)
            print(
                f'img_pred-shape: {img_pred.shape}\timg_pred.min(): {img_pred.min()}\timg_pred.max(): {img_pred.max()}')

            return

    # path_k_models = np.sort([x for x in glob(os.path.join(path_models, "*/*")) if os.path.isdir(x)])
    # print(f'\n{len(path_k_models)} available models:\n', path_k_models)

    # df_scores_all = pd.DataFrame(
    #     columns=['Accuracy', 'Precision', 'Recall', 'F-1', 'TruePositive', 'FalsePositive', 'FalseNegative',
    #              'TrueNegative', 'dice-loss', 'entropy-loss'])

    # iterate about all test-data-k-folds per model
    # for idx, row in df_k_fold_abbrevations.iterrows():

        # (0) check if there are enough models for each test-data-permutation
        # if idx + 1 > len(path_k_models):
        #     print(
        #         f'\n\tABORT EVALUATION: number of iteration ({idx + 1}) > number of available models ({len(path_k_models)})')
        #     break

        # (1) load the corresponding model and config file to the test data set
        # path_single_model = glob(os.path.join(path_k_models[idx], '*.hdf5'))[0]
        # print(f'\nmodel-id: {idx}\tmodel-path: {path_single_model}')
        #
        # path_single_config = glob(os.path.join(path_k_models[idx], 'mdl_hyperparameter.json'))[0]
        # print(f'model-id: {idx}\tconfig-path: {path_single_config}')

        # (2) build the model
        # model = SegNet.MySegNet(modelName='FatTissueSegmentation',
        #                         create_new_model=path_single_model,
        #                         config_path=path_single_config,
        #                         verbose=1)

        # input_model_path = config["input_model_path"] if "input_model_path" in config else model_config[
        #     'output_model_path']
        # model_img_shape = tuple(model_config["image_shape"])

        # iterate about each single test sample
        # for abbrevation in row:

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
            img_pred = model.predict_sample(img_origin=img_orig, savePath=None, save_probMap_image=0, verbose=0)
            print(
                f'img_pred-shape: {img_pred.shape}\timg_pred.min(): {img_pred.min()}\timg_pred.max(): {img_pred.max()}')

            # (5) save thresholded image
            path_img_thresh = os.path.join('/asbdata/Philipp/FungIdent/image_data/cross_validation/',
                                           path_k_models[idx].split('/')[2], path_k_models[idx].split('/')[3],
                                           path_k_models[idx].split('/')[4])
            if not os.path.exists(path_img_thresh):
                os.makedirs(path_img_thresh)
                print('\tcreate mod_dir:', path_img_thresh)

            path_img_thresh = os.path.join(path_img_thresh, orig_path.split('/')[-1])
            # tif.imsave(path_img_thresh, img_pred)
            cv2.imwrite(path_img_thresh, img_pred)
            print('\tsave img_pred to:\t', path_img_thresh.split('/')[-5:])

            # (6) evaluate the predicted sample with the specified annotation image
            # df_scores_single = perform_single_evaluation(true_path, '', verbose=1, img_pred=img_pred)
            df_scores_single = perform_single_evaluation(img_true, '', verbose=1, img_pred=img_pred,
                                                         img_true_path=true_path)

            # (7) append the scores with a collections of all scores
            df_scores_all = df_scores_all.append(df_scores_single)

            # store the dataframe with all evaluations after each iteration
            path_df_single = os.path.join(path_models, 'eval_model_{0}_{1}.csv'.format(idx, abbrevation))
            df_scores_all.to_csv(path_df_single)
            print('\tsave df_single to:\t', path_df_single)

    print('\nFinish the whole evaluation with the following scores:\n\n', df_scores_all)
    return df_scores_all
