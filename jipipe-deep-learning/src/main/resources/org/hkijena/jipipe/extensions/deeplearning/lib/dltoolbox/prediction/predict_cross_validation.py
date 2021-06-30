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
import math
import tifffile as tif
import cv2
from dltoolbox.prediction.predict_samples import predict_samples
from dltoolbox import utils


def predict_cross_validation(model_config, config, model=None):
    """
    Predicts multiple models as a cross-validation with a corresponding information table
    Args:
        model_config: the model config
        config: the config for this prediction
        model: An existing model (optional)

    Returns: Prediction results (list of predictions, list of input files)

    """

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
    for model_id, row in df_k_fold_abbrevations.iterrows():
        # get the match of the model_name within all parent directories of available model paths
        single_model_path = [elem for elem in available_model_paths if str(row['model_name']) in elem]

        df_k_fold_abbrevations.loc[model_id, 'model_path'] = single_model_path

    print('[Predict.cross-validation] info_table:\n', df_k_fold_abbrevations)

    for model_id, row in tqdm(df_k_fold_abbrevations.iterrows(), total=df_k_fold_abbrevations.shape[0]):

        print(f"\n[Predict.cross-validation] model_id - # models: [ {model_id} / {df_k_fold_abbrevations.shape[0]} ]")

        # (1) load the corresponding model and config file for the test images
        for (dirpath, dirnames, filenames) in os.walk(row['model_path']):
            model_files_path = [os.path.join(dirpath, file) for file in filenames]

        input_model_path = [elem for elem in model_files_path if str('model.hdf5') in elem][0]
        input_model_json_path = [elem for elem in model_files_path if str('model.json') in elem][0]
        model_config_path = [elem for elem in model_files_path if str('model-config.json') in elem][0]

        with open(model_config_path, "r") as f:
            model_config = json.load(f)

        print(f"[Predict.cross-validation] model-path: {input_model_path}")

        # (2) load and build the model
        model = utils.load_and_compile_model(model_config, input_model_path, model)

        input_dirs = []

        # (3) collect all test image path per model
        for i, abbreviation in enumerate(row[['0', '1', '2']]):
            print(f"[Predict.cross-validation] abbreviation for <{i}>-th original image path per model: {abbreviation}")

            if type(abbreviation) == np.float and math.isnan(abbreviation):
                print(f"[Predict.cross-validation] CAUTION: continue - could not find abbreviation: {abbreviation}")
                continue

            # (4) get the original image path and place it in the config file to only predict this image
            orig_path = [x for x in input_images if abbreviation in x][0]
            print(f"[Predict.cross-validation] <{i}>-th original image path per model: {orig_path}")

            input_dirs.append(orig_path)

        # (4) set all input directories of the test images for the current model
        config['input_dir'] = input_dirs

        # TODO: tmp - später löschen
        if model_id in [0,1,2,3,4,5,6,7,8,9,10]:
            continue

        # (5) on-the-fly prediction of the test images with the corresponding model
        imgs_pred, imgs_filenames = predict_samples(model_config=model_config, config=config, model=model)
        print(f"[Predict.cross-validation] images: {len(imgs_pred)}\timgs_pred[0].min(): {imgs_pred[0].min()}\timgs_pred[0].max(): {imgs_pred[0].max()}")