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
        y_true: true labels
        y_pred: model prediction

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
        y_true: true labels
        y_pred: model prediction

    Returns: Score

    """
    loss = 1 - dice_coeff(y_true, y_pred)
    return loss


def bce_dice_loss(y_true, y_pred):
    """
    binary-cross-entropy with dice loss as regularization term
    Args:
        y_true: true labels
        y_pred: model prediction

    Returns: Score

    """
    # TODO: alle regularisierungs-terme mit **kwargs dynamisch ergänzen
    loss = losses.binary_crossentropy(y_true, y_pred) + dice_loss(y_true, y_pred)
    return loss


def ce_dice_loss(y_true, y_pred):
    """
    categorical-cross-entropy with dice loss as regularization term
    Args:
        y_true: true labels
        y_pred: model prediction

    Returns: Score

    """
    loss = losses.categorical_crossentropy(y_true, y_pred) + dice_loss(y_true, y_pred)
    return loss


def determine_standard_measure(y_true, y_pred):
    """

    Args:
        y_true: true labels
        y_pred: model prediction

    Returns:

    """
    TP = sum(y_true == y_pred)
    FP = sum((y_pred == 1) & (y_true != y_pred))
    TN = sum((y_true == y_pred) & (y_true == 0) & (y_pred == 0))
    FN = sum((y_pred == 0) & (y_true != y_pred))

    return TP, FP, TN, FN


def determine_advanded_measures(TP, FP, TN, FN):
    """

    Args:
        TP: true positives
        FP: false positives
        TN: true negatives
        FN: false negatives

    Returns:

    """
    accuracy = (TP + TN) / (TP + TN + FP + FN)
    precision = TP / (TP + FP)
    recall = TP / (TP + FN)
    F_1 = (2 * (precision * recall)) / (precision + recall)

    return accuracy, precision, recall, F_1


def Jaccard_index(true_bb, pred_bb):
    """

    Args:
        true_bb: ground truth bounding box
        pred_bb: predicted bounding box

    Returns:

    """

    true_bb = tf.stack([
        true_bb[:, :, :, :, 0] - true_bb[:, :, :, :, 2] / 2.0,
        true_bb[:, :, :, :, 1] - true_bb[:, :, :, :, 3] / 2.0,
        true_bb[:, :, :, :, 0] + true_bb[:, :, :, :, 2] / 2.0,
        true_bb[:, :, :, :, 1] + true_bb[:, :, :, :, 3] / 2.0])
    true_bb = tf.transpose(true_bb, [1, 2, 3, 4, 0])
    pred_bb = tf.stack([
        pred_bb[:, :, :, :, 0] - pred_bb[:, :, :, :, 2] / 2.0,
        pred_bb[:, :, :, :, 1] - pred_bb[:, :, :, :, 3] / 2.0,
        pred_bb[:, :, :, :, 0] + pred_bb[:, :, :, :, 2] / 2.0,
        pred_bb[:, :, :, :, 1] + pred_bb[:, :, :, :, 3] / 2.0])
    pred_bb = tf.transpose(pred_bb, [1, 2, 3, 4, 0])
    area = tf.maximum(0.0,
                      tf.minimum(true_bb[:, :, :, :, 2:], pred_bb[:, :, :, :, 2:]) -
                      tf.maximum(true_bb[:, :, :, :, :2], pred_bb[:, :, :, :, :2]))

    intersection_area = area[:, :, :, :, 0] * area[:, :, :, :, 1]
    gt_bb_area = (true_bb[:, :, :, :, 2] - true_bb[:, :, :, :, 0]) * \
                 (true_bb[:, :, :, :, 3] - true_bb[:, :, :, :, 1])
    pred_bb_area = (pred_bb[:, :, :, :, 2] - pred_bb[:, :, :, :, 0]) * \
                   (pred_bb[:, :, :, :, 3] - pred_bb[:, :, :, :, 1])
    union_area = tf.maximum(gt_bb_area + pred_bb_area - intersection_area, 1e-10)
    iou = tf.clip_by_value(intersection_area / union_area, 0.0, 1.0)

    return iou


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


def focal_loss(y_true, y_pred):
    gamma = 2
    alpha = 0.75
    eps = 1e-12

    # improve the stability of the focal loss and see issues 1 for more information
    y_pred = K.clip(y_pred, eps, 1. - eps)
    pt_1 = tf.where(tf.equal(y_true, 1), y_pred, tf.ones_like(y_pred))
    pt_0 = tf.where(tf.equal(y_true, 0), y_pred, tf.zeros_like(y_pred))
    focal_loss_fixed = -K.sum(alpha * K.pow(1. - pt_1, gamma) * K.log(pt_1)) - K.sum(
        (1 - alpha) * K.pow(pt_0, gamma) * K.log(1. - pt_0))

    return focal_loss_fixed

# TODO: averaged F1-score over thresholds from 1 to 5 pixelsaround the ground truth
# get this metric here: https://www.cv-foundation.org/openaccess/content_cvpr_2016/papers/Perazzi_A_Benchmark_Dataset_CVPR_2016_paper.pdf
