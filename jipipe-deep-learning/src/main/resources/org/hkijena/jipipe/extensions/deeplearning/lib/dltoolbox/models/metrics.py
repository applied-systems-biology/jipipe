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
from sklearn.metrics import matthews_corrcoef

def get_metrics(model_type, num_classes):
    """
    Define multiple metrics dependent on the model-type
    Args:
        model_type: the model type as string, given in model-config.

    Returns: all tensorflow metrics

    """

    if model_type == 'segmentation':
        metrics = [
            tf.keras.metrics.TruePositives(name='tp'),
            tf.keras.metrics.FalsePositives(name='fp'),
            tf.keras.metrics.TrueNegatives(name='tn'),
            tf.keras.metrics.FalseNegatives(name='fn'),
            tf.keras.metrics.AUC(name='prc', curve='PR'),  # precision-recall curve
            tf.keras.metrics.MeanIoU(num_classes=num_classes),
            dice_coeff,
            IoU
        ]
    elif model_type == 'classification':
        metrics = [
            tf.keras.metrics.TruePositives(name='tp'),
            tf.keras.metrics.FalsePositives(name='fp'),
            tf.keras.metrics.TrueNegatives(name='tn'),
            tf.keras.metrics.FalseNegatives(name='fn'),
            tf.keras.metrics.Accuracy(name='accuracy'),
            tf.keras.metrics.Precision(name='precision'),
            tf.keras.metrics.Recall(name='recall'),
            tf.keras.metrics.AUC(name='auc'),
            tf.keras.metrics.AUC(name='prc', curve='PR'),  # precision-recall curve
        ]
    else:
        raise AttributeError("Unsupported model-type for metrics: " + str(model_type))

    return metrics


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
    Custom defined binary-cross-entropy with dice loss & IoU as regularization term
    Args:
        y_true: true labels
        y_pred: model prediction

    Returns: Score

    """

    # add a small epsilon to the predictions
    EPSILON = 1e-05
    y_pred = y_pred + EPSILON

    loss = tf.keras.losses.binary_crossentropy(y_true, y_pred) + dice_loss(y_true, y_pred)

    return loss


def ce_dice_loss(y_true, y_pred):
    """
    Custom defined categorical-cross-entropy with dice loss & IoU as regularization term
    Args:
        y_true: true labels
        y_pred: model prediction

    Returns: Score

    """

    # add a small epsilon to the predictions
    EPSILON = 1e-05
    y_pred = y_pred + EPSILON

    loss = tf.keras.losses.categorical_crossentropy(y_true, y_pred) + dice_loss(y_true, y_pred)
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


def IoU(y_true, y_pred):
    """
    Jaccard similarity coefficient score.

    The Jaccard index [1], or Jaccard similarity coefficient, defined as the size of the intersection divided
    by the size of the union of two label sets, is used to compare set of predicted labels
    for a sample to the corresponding set of labels in y_true.

    Args:
        true_bb: true labels
        pred_bb: model prediction

    Returns:
        scorefloat (if average is not None) or array of floats, shape = [n_unique_labels]

    """

    y_true_f = tf.reshape(y_true, [-1])
    y_pred_f = tf.reshape(y_pred, [-1])

    intersection = tf.reduce_sum(y_true_f * y_pred_f)
    union = (tf.reduce_sum(y_true_f) + tf.reduce_sum(y_pred_f)) - intersection

    score = intersection / union

    return score


def IoU_loss(y_true, y_pred):
    """
    Jaccard similarity loss.

    The Jaccard index [1], or Jaccard similarity coefficient, defined as the size of the intersection divided
    by the size of the union of two label sets, is used to compare set of predicted labels
    for a sample to the corresponding set of labels in y_true.

    Args:
        y_true: true labels
        y_pred: model prediction

    Returns: Score

    """

    loss = 1 - IoU(y_true, y_pred)
    return loss


def MMC(y_true, y_pred):
    """
    The Matthews correlation coefficient is used in machine learning as a measure of the quality of binary and
    multiclass classifications. It takes into account true and false positives and negatives and is generally
    regarded as a balanced measure which can be used even if the classes are of very different sizes.
    The MCC is in essence a correlation coefficient value between -1 and +1. A coefficient of +1 represents
    a perfect prediction, 0 an average random prediction and -1 an inverse prediction.
    The statistic is also known as the phi coefficient. [source: Wikipedia]

    Args:
        y_true: true labels
        y_pred: model prediction

    Returns:
        mcc. float. The Matthews correlation coefficient
        (+1 represents a perfect prediction, 0 an average random prediction and -1 and inverse prediction)
    """

    '''
    calculate the Matthew correlation coefficient
    '''

    ### tensorflow

    # threshold = 0.5
    #
    # predicted = tf.cast(tf.greater(y_pred, threshold), tf.float32)
    #
    # true_pos = tf.math.count_nonzero(predicted * y_true)
    # true_neg = tf.math.count_nonzero((predicted - 1) * (y_true - 1))
    # false_pos = tf.math.count_nonzero(predicted * (y_true - 1))
    # false_neg = tf.math.count_nonzero((predicted - 1) * y_true)
    #
    # x = tf.cast((true_pos + false_pos) * (true_pos + false_neg)
    #             * (true_neg + false_pos) * (true_neg + false_neg), tf.float32)
    #
    # score = tf.cast((true_pos * true_neg) - (false_pos * false_neg), tf.float32) / tf.sqrt(x)

    ### sklearn
    score = matthews_corrcoef(y_true, y_pred)

    return score


def MMC_loss(y_true, y_pred):
    """
    The Matthews correlation coefficient - loss. is used in machine learning as a measure of the quality of binary and
    multiclass classifications. It takes into account true and false positives and negatives and is generally
    regarded as a balanced measure which can be used even if the classes are of very different sizes.
    The MCC -loss is in essence a correlation coefficient value between -1 and +1. A coefficient of -1 represents
    a perfect prediction, 0 an average random prediction and +1 an inverse prediction.

    Args:
        y_true: true labels
        y_pred: model prediction

    Returns:
        mcc. float. The Matthews correlation coefficient
        (+1 represents a perfect prediction, 0 an average random prediction and -1 and inverse prediction)
    """

    score = matthews_corrcoef(y_true, y_pred)

    loss = score / -1

    return loss


def focal_loss(y_true, y_pred):

    gamma = 2
    alpha = 0.75
    eps = 1e-12

    # improve the stability of the focal loss and see issues 1 for more information
    y_pred = tf.clip_by_value(y_pred, eps, 1. - eps)
    pt_1 = tf.where(tf.equal(y_true, 1), y_pred, tf.ones_like(y_pred))
    pt_0 = tf.where(tf.equal(y_true, 0), y_pred, tf.zeros_like(y_pred))
    focal_loss_fixed = - tf.reduce_sum(alpha * tf.math.pow(1. - pt_1, gamma) * tf.math.log(pt_1)) - tf.reduce_sum(
        (1 - alpha) * tf.math.pow(pt_0, gamma) * tf.math.log(1. - pt_0))

    return focal_loss_fixed

# TODO: averaged F1-score over thresholds from 1 to 5 pixelsaround the ground truth
# get this metric here: https://www.cv-foundation.org/openaccess/content_cvpr_2016/papers/Perazzi_A_Benchmark_Dataset_CVPR_2016_paper.pdf
