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

from dltoolbox import utils


# import json
# from glob import glob
# import time
# from datetime import datetime
# import cv2
import numpy as np
# import skimage
# from scipy import ndimage
# import tifffile as tif
# import matplotlib.pyplot as plt
# import matplotlib as mpl
# import matplotlib.image as mpimg
# import pandas as pd


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
                                verbose=1)

        # iterate about each single test sample
        for abbrevation in row:

            if type(abbrevation) == np.float and math.isnan(abbrevation):
                print('CAUTION: continue - could not find abbreviaton:', abbrevation)
                continue

            # (3) load the original image
            orig_path = [x for x in path_all_files_origin if abbrevation in x][0]
            true_path = [x for x in path_all_files_true if abbrevation in x][0]

            try:
                img_orig = tif.imread(orig_path)
            except:
                img_orig = cv2.imread(orig_path)
            print('\norig_path:\t', orig_path.split('/')[-2:], 'orig-shape:\t', img_orig.shape)
            try:
                img_true = tif.imread(true_path, )
            except:
                img_true = cv2.imread(true_path, 0)

                # if label image is not a binary image: binarize it:
            if len(np.unique(img_true)) != 2:
                print('BEFORE: binarize label image - number of different intensities:', len(np.unique(img_true)))
                img_true = binarizeAnnotation(img_true, use_otsu=True, convertToGray=True)
                print('AFTER: binarize label image - number of different intensities:', len(np.unique(img_true)))

            print('true_path:\t', true_path.split('/')[-2:], 'orig-shape:\t', img_true.shape, img_true.dtype)
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
