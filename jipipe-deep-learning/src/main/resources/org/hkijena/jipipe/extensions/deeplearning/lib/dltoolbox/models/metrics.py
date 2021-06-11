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

Script to define costum-made metrcis
"""

import numpy as np
import tensorflow as tf
from keras import losses
from sklearn.metrics import matthews_corrcoef


def dice_coeff(y_true, y_pred):
    """
    Sørensen-Dice coefficient
    Args:
        y_true:
        y_pred:

    Returns: Score

    """
    smooth = 1.
    # Flatten
    y_true_f = tf.reshape(y_true, [-1])
    y_pred_f = tf.reshape(y_pred, [-1])
    intersection = tf.reduce_sum(y_true_f * y_pred_f)
    score = (2. * intersection + smooth) / (tf.reduce_sum(y_true_f) + tf.reduce_sum(y_pred_f) + smooth)
    return score


def dice_loss(y_true, y_pred):
    """
    Sørensen-Dice loss: 1 - Sørensen-Dice coefficient
    Args:
        y_true:
        y_pred:

    Returns: Score

    """
    loss = 1 - dice_coeff(y_true, y_pred)
    return loss


def bce_dice_loss(y_true, y_pred):
    """
    binary-cross-entropy with dice loss as regularization term
    Args:
        y_true:
        y_pred:

    Returns: Score

    """
    # TODO: alle regularisierungs-terme mit **kwargs dynamisch ergänzen
    loss = losses.binary_crossentropy(y_true, y_pred) + dice_loss(y_true, y_pred)
    return loss


def ce_dice_loss(y_true, y_pred):
    """
    categorical-cross-entropy with dice loss as regularization term
    Args:
        y_true:
        y_pred:

    Returns: Score

    """
    loss = losses.categorical_crossentropy(y_true, y_pred) + dice_loss(y_true, y_pred)
    return loss

def MCC(y_true, y_pred):
    """
    calculate the Matthews correlation coefficient (MCC)
    Args:
        y_true:
        y_pred:

    Returns:
        float: The final score.
    """


    '''
    calculate the Matthew correlation coefficient
    '''
    
    # TP = df_tmp["TruePositive"]
    # FP = df_tmp["FalsePositive"]
    # FN = df_tmp["FalseNegative"]
    # TN = df_tmp["TrueNegative"]
    
    # counter = (TN * TP) - (FP * FN)
    # denominator = np.sqrt( (TN + FN)*(FP + TP)*(TN + FP)*(FN + TP) )
    
    # return counter / denominator

    return matthews_corrcoef(y_true, y_pred)



# TODO: averaged F1-score over thresholds from 1 to 5 pixelsaround the ground truth
# get this metric here: https://www.cv-foundation.org/openaccess/content_cvpr_2016/papers/Perazzi_A_Benchmark_Dataset_CVPR_2016_paper.pdf
