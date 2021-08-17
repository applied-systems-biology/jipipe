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

Script to define custom-made callbacks
"""

"""

https://www.tensorflow.org/guide/keras/custom_callback

Template to define the custom defined callback and where it is possible to get a callback:
 
class CustomCallback(tf.keras.keras.callbacks.Callback):
    def on_train_begin(self, logs=None):
        keys = list(logs.keys())
        print("Starting training; got log keys: {}".format(keys))

    def on_train_end(self, logs=None):
        keys = list(logs.keys())
        print("Stop training; got log keys: {}".format(keys))

    def on_epoch_begin(self, epoch, logs=None):
        keys = list(logs.keys())
        print("Start epoch {} of training; got log keys: {}".format(epoch, keys))

    def on_epoch_end(self, epoch, logs=None):
        keys = list(logs.keys())
        print("End epoch {} of training; got log keys: {}".format(epoch, keys))

    def on_test_begin(self, logs=None):
        keys = list(logs.keys())
        print("Start testing; got log keys: {}".format(keys))

    def on_test_end(self, logs=None):
        keys = list(logs.keys())
        print("Stop testing; got log keys: {}".format(keys))

    def on_predict_begin(self, logs=None):
        keys = list(logs.keys())
        print("Start predicting; got log keys: {}".format(keys))

    def on_predict_end(self, logs=None):
        keys = list(logs.keys())
        print("Stop predicting; got log keys: {}".format(keys))

    def on_train_batch_begin(self, batch, logs=None):
        keys = list(logs.keys())
        print("...Training: start of batch {}; got log keys: {}".format(batch, keys))

    def on_train_batch_end(self, batch, logs=None):
        keys = list(logs.keys())
        print("...Training: end of batch {}; got log keys: {}".format(batch, keys))

    def on_test_batch_begin(self, batch, logs=None):
        keys = list(logs.keys())
        print("...Evaluating: start of batch {}; got log keys: {}".format(batch, keys))

    def on_test_batch_end(self, batch, logs=None):
        keys = list(logs.keys())
        print("...Evaluating: end of batch {}; got log keys: {}".format(batch, keys))

    def on_predict_batch_begin(self, batch, logs=None):
        keys = list(logs.keys())
        print("...Predicting: start of batch {}; got log keys: {}".format(batch, keys))

    def on_predict_batch_end(self, batch, logs=None):
        keys = list(logs.keys())
        print("...Predicting: end of batch {}; got log keys: {}".format(batch, keys))
   
   
### callbacks are also possible for predictions and evaluation, next to the training:

# training:
model.fit(x_train, y_train, batch_size=128, epochs=1, callbacks=[CustomCallback()])

# evaluation:
res = model.evaluate(x_test, y_test, batch_size=128, verbose=0, callbacks=[CustomCallback()])

# prediction:
res = model.predict(x_test, batch_size=128, callbacks=[CustomCallback()])
        
"""

import os
import io as io_system
import numpy as np
import pandas as pd
from matplotlib import pyplot as plt
import cv2
from skimage import io, color
from sklearn.metrics import confusion_matrix
import tensorflow as tf
from dltoolbox.evaluation import visualize


def get_callbacks(input_model_path, log_dir, unet_model, show_plots, val_data=None):
    """
    Provide all tensorflow and custom defined callbacks dependent on a verbose level.
    Args:
        input_model_path: save path for the model checkpoint callback
        log_dir: save path for the logging directory
        unet_model: True if model is a unet model with image shape as output
        show_plots: boolean whether the plots will be shown or not during the training
        val_data: list of validation data for inputs and targets. Necessary if <show_plots> = True

    Returns: All necessary callbacks.

    """

    # create all tensorflow callbacks
    tensorboard_callback = tf.keras.callbacks.TensorBoard(log_dir=log_dir,
                                                          histogram_freq=0,
                                                          write_graph=False,
                                                          write_images=False)
    earlyStopping_callback = tf.keras.callbacks.EarlyStopping(monitor='val_loss',
                                                              patience=200,
                                                              verbose=1,
                                                              mode='min')
    modelCheckpoint_callback = tf.keras.callbacks.ModelCheckpoint(input_model_path,
                                                                  save_best_only=True,
                                                                  save_freq='epoch',
                                                                  monitor='val_loss',
                                                                  mode='min')
    reduceLr_callback = tf.keras.callbacks.ReduceLROnPlateau(monitor='val_loss',
                                                             factor=0.85,
                                                             patience=50,
                                                             min_lr=0.00001,
                                                             verbose=1)
    csv_logger_callback = tf.keras.callbacks.CSVLogger(os.path.join(log_dir, 'training.log'),
                                                       separator=',',
                                                       append=False)
    terminate_on_nan = tf.keras.callbacks.TerminateOnNaN()
    history_callback = tf.keras.callbacks.History()

    # collect all callbacks
    callbacks = {

        'history': history_callback,
        'csv_logger': csv_logger_callback,
        'model_checkpoint': modelCheckpoint_callback,
        'early_stopping': earlyStopping_callback,
        'reduce_learning_rate': reduceLr_callback,
        'terminate_on_nan': terminate_on_nan,
        'tensorboard': tensorboard_callback

    }

    # show plots from callbacks during the training with custom defined callbacks
    if show_plots:

        if unet_model:
            print(f'[get_callbacks] Create UNET custom callbacks ...')

            display_prediction = DisplayPrediction(log_dir=log_dir, validation_data=val_data)

            callbacks['display_prediction'] = display_prediction

        else:
            print(f'[get_callbacks] Create NON UNET custom callbacks ...')

            confusion_matrixCallback = ConfusionMatrixCallback(log_dir=log_dir, validation_data=val_data)
            grad_cam = Grad_CAMCallback(log_dir=log_dir, validation_data=val_data)

            callbacks['confusion_matrix'] = confusion_matrixCallback
            callbacks['grad_cam'] = grad_cam

    print(f'[Get callbacks] get number of callbacks: <{len(callbacks)}>')

    return callbacks


### Define custom callbacks ###


class LossAndErrorPrintingCallback(tf.keras.callbacks.Callback):
    """
    Print the loss for each batch separated.
    """

    def on_train_batch_end(self, batch, logs=None):
        print("For batch {}, loss is {:7.2f}.".format(batch, logs["loss"]))

    def on_test_batch_end(self, batch, logs=None):
        print("For batch {}, loss is {:7.2f}.".format(batch, logs["loss"]))

    def on_epoch_end(self, epoch, logs=None):
        print(
            "The average loss for epoch {} is {:7.2f} "
            "and mean absolute error is {:7.2f}.".format(
                epoch, logs["loss"], logs["mean_absolute_error"]
            )
        )


class ConfusionMatrixCallback(tf.keras.callbacks.Callback):
    """
    Custom callback for store confusion matrix (for classification models)

    Arguments:
        log_dir: logging directory
        validation_data: the validation data [0]=input , [1]=label
    """

    def __init__(self, log_dir, validation_data):
        super(ConfusionMatrixCallback, self).__init__()

        self.validation_data = validation_data
        self.conf_matrix_log_dir = os.path.join(log_dir, 'confusion_matrix')

        # create the log directory for confusion matrices
        if not os.path.exists(self.conf_matrix_log_dir):
            os.makedirs(self.conf_matrix_log_dir)
            print('[ConfusionMatrixCallback] create folder for confusion matrix logs:', self.conf_matrix_log_dir)

    def on_epoch_end(self, epoch, logs=None):

        # Use the model to predict the values from the validation dataset.
        x_valid = self.validation_data[0]

        y_pred = self.model.predict(x_valid).argmax(axis=1)
        y_true = self.validation_data[1].argmax(axis=1)

        # calculate the confusion matrix
        conf_matrix_orig = confusion_matrix(y_true=y_true, y_pred=y_pred)

        # save confusion matrix with absolute numbers as csv. - no mirror of dataframe in original confusion_matrix
        df_conf_matrix_orig = pd.DataFrame(conf_matrix_orig)
        df_conf_matrix_orig.to_csv(os.path.join(self.conf_matrix_log_dir, 'confusion_matrix_orig.csv'))

        # normalize the confusion matrix
        conf_matrix_norm = np.around(conf_matrix_orig.astype('float') / conf_matrix_orig.sum(axis=1)[:, np.newaxis],
                                     decimals=5)

        # save confusion matrix with normalized values as csv
        df_conf_matrix_norm = pd.DataFrame(conf_matrix_norm)
        df_conf_matrix_norm.to_csv(os.path.join(self.conf_matrix_log_dir, 'confusion_matrix_norm.csv'))

        # INFO: show confusion matrices every 10-th epoch, otherwise skip plot procedure
        if epoch % 10 == 0:

            # build the figure with a absoulte / normalized confusion matrix
            fig, ax_arr = plt.subplots(1, 2, figsize=(18, 8))
            ax1, ax2 = ax_arr.ravel()

            ax1.matshow(conf_matrix_orig, cmap=plt.cm.Blues, alpha=0.3)
            for i in range(conf_matrix_orig.shape[0]):
                for j in range(conf_matrix_orig.shape[1]):
                    ax1.text(x=j, y=i, s=conf_matrix_orig[i, j], va='center', ha='center', size='xx-large')

            ax1.set_xlabel('Predictions', fontsize=20)
            ax1.set_ylabel('True labels', fontsize=20)
            ax1.set_title('Confusion Matrix', fontsize=25)

            ax2.matshow(conf_matrix_norm, cmap=plt.cm.Blues, alpha=0.3)
            for i in range(conf_matrix_norm.shape[0]):
                for j in range(conf_matrix_norm.shape[1]):
                    ax2.text(x=j, y=i, s=np.around(conf_matrix_norm[i, j], decimals=2),
                             va='center', ha='center', size='xx-large')

            ax2.set_xlabel('Predictions', fontsize=20)
            ax2.set_ylabel('True labels', fontsize=20)
            ax2.set_title('Normalized Confusion Matrix', fontsize=25)

            plt.tight_layout()

            # save the figure
            buf = io_system.BytesIO()
            plt.savefig(buf, format='png')
            # convert figure to numpy array
            fig.canvas.draw()
            # now we can save it to a numpy array.
            data = np.fromstring(fig.canvas.tostring_rgb(), dtype=np.uint8, sep='')
            data = data.reshape(fig.canvas.get_width_height()[::-1] + (3,))
            save_path_figure = os.path.join(self.conf_matrix_log_dir, f'confusion_matrix_epoch_{epoch}_.png')
            io.imsave(save_path_figure, data)
            buf.close()
            plt.show()
            plt.close(fig)


class Grad_CAMCallback(tf.keras.callbacks.Callback):
    """
    Custom callback to calculate the activation maps (for classification models)

    Arguments:
        log_dir: logging directory
        validation_data: the validation data [0]=input , [1]=label
    """

    def __init__(self, log_dir, validation_data):
        super(Grad_CAMCallback, self).__init__()

        self.validation_data = validation_data
        self.grad_cam_log_dir = os.path.join(log_dir, 'Grad-CAM')

        # create the log directory for confusion matrices
        if not os.path.exists(self.grad_cam_log_dir):
            os.makedirs(self.grad_cam_log_dir)
            print('[Grad_CAMCallback] create folder for Grad-CAM logs:', self.grad_cam_log_dir)

    def on_epoch_end(self, epoch, logs=None):

        # INFO: show Grad-CAM every 10-th epoch
        if epoch % 10 == 0:

            x_valid = self.validation_data[0]
            y_valid = self.validation_data[1]

            # define random index
            rd_idx = np.random.randint(0, high=len(x_valid) - 1)

            image = x_valid[rd_idx]
            image = np.expand_dims(image, axis=0)

            label_value = y_valid[rd_idx].argmax()

            print(f'[Grad_CAMCallback] Image number [ {rd_idx} / {len(x_valid)} ] with shape: {image.shape}')

            # use network to make predictions on input and find class label index with largest corresponding probability
            preds = self.model.predict(image)
            i = np.argmax(preds[0])

            print('[Grad_CAMCallback] Nr-label: <{}> with probability: {:.2f}%'.format(i, preds[0][i] * 100))

            image_label = "{}: {:.2f}%".format(i, preds[0][i] * 100)

            cam = visualize.GradCAM(self.model, i)
            heatmap = cam.compute_heatmap(image)

            # overlay heatmap on top of the image
            (heatmap, output) = cam.overlay_heatmap(heatmap, image, alpha=0.5)

            # draw the predicted label on the output image
            cv2.rectangle(output, (0, 0), (340, 40), (0, 0, 0), -1)
            cv2.putText(output, image_label, (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)

            # display the original image and resulting heatmap and output image to our screen
            image = np.squeeze(image)

            if len(image.shape) == 2 and len(heatmap.shape) == 3:
                image = color.gray2rgb(image)

            output = np.hstack([image, heatmap, output])

            # create the figure
            fig = plt.figure(figsize=(21, 7))
            plt.imshow(output)
            title = "true: {} - prediction {} with probability {:.2f}%".format(label_value, i, preds[0][i] * 100)
            plt.title(title, fontsize=20)
            plt.tight_layout()

            # save the figure
            buf = io_system.BytesIO()
            plt.savefig(buf, format='png')
            # convert figure to numpy array
            fig.canvas.draw()
            # now we can save it to a numpy array.
            data = np.fromstring(fig.canvas.tostring_rgb(), dtype=np.uint8, sep='')
            data = data.reshape(fig.canvas.get_width_height()[::-1] + (3,))
            save_path_figure = os.path.join(self.grad_cam_log_dir, f'grad_camp_epoch_{epoch}_.png')
            io.imsave(save_path_figure, data)
            buf.close()
            plt.show()
            plt.close(fig)


class DisplayPrediction(tf.keras.callbacks.Callback):
    """
    Display an input with its according label images and prediction. (for segmentation models)

    Arguments:
        log_dir: logging directory
        validation_data: the validation data [0]=input , [1]=label
    """

    def __init__(self, log_dir, validation_data):
        super(DisplayPrediction, self).__init__()

        self.validation_data = validation_data
        self.display_prediction_log_dir = os.path.join(log_dir, 'DisplayPrediction')

        # create the log directory for confusion matrices
        if not os.path.exists(self.display_prediction_log_dir):
            os.makedirs(self.display_prediction_log_dir)
            print('[DisplayPrediction] create folder for display predictions logs:', self.display_prediction_log_dir)

    def on_epoch_end(self, epoch, logs=None):

        # INFO: show confusion matrices every 10-th epoch, otherwise skip plot procedure
        if epoch % 10 == 0:

            # from IPython.display import clear_output
            # clear_output(wait=True)

            x_valid = self.validation_data[0]
            y_valid = self.validation_data[1]

            # define random index
            rd_idx = np.random.randint(0, high=len(x_valid) - 1)

            # get input and label image
            image = x_valid[rd_idx]
            image = np.expand_dims(image, axis=0)

            mask = y_valid[rd_idx]

            print(f'[DisplayPrediction] Image number [ {rd_idx} / {len(x_valid)} ] with shape: {image.shape}')

            # use network to make predictions on input and find class label index with largest corresponding probability
            pred_mask = self.model.predict(image)

            # prepare images for display
            image = np.squeeze(image)
            mask = np.squeeze(mask)
            pred_mask = np.squeeze(pred_mask)

            # create the figure
            fig, ax_arr = plt.subplots(1, 3, sharey=True, figsize=(18, 8))
            ax1, ax2, ax3 = ax_arr.ravel()

            if len(image.shape) == 3:
                ax1.imshow(image)
            else:
                ax1.imshow(image, cmap='gray')
            ax1.set_title("Input image with green label contour", fontsize=15)
            ax1.contour(mask, colors='green', linewidths=1)

            ax2.imshow(mask, cmap='gray')
            ax2.set_title("True mask", fontsize=15)

            ax3.imshow(pred_mask, cmap='gray')
            ax3.set_title("Predicted Mask", fontsize=15)

            plt.tight_layout()

            # save the figure
            buf = io_system.BytesIO()
            plt.savefig(buf, format='png')
            # convert figure to numpy array
            fig.canvas.draw()
            # now we can save it to a numpy array.
            data = np.fromstring(fig.canvas.tostring_rgb(), dtype=np.uint8, sep='')
            data = data.reshape(fig.canvas.get_width_height()[::-1] + (3,))
            save_path_figure = os.path.join(self.display_prediction_log_dir, f'display_prediction_epoch_{epoch}_.png')
            io.imsave(save_path_figure, data)
            buf.close()
            plt.show()
            plt.close(fig)


### TODO dropout reducer implementieren:

# class MyDropout(tf.keras.layers.Layer):
#     # @interfaces.legacy_dropout_support
#     def __init__(self, rate, noise_shape=None, seed=None, **kwargs):
#         super(MyDropout, self).__init__(**kwargs)
#         self.rate = K.variable(min(1., max(0., rate)))
#         self.noise_shape = noise_shape
#         self.seed = seed
#         self.supports_masking = True
#
#     def _get_noise_shape(self, inputs):
#         if self.noise_shape is None:
#             return self.noise_shape
#
#         symbolic_shape = K.shape(inputs)
#         noise_shape = [symbolic_shape[axis] if shape is None else shape
#                        for axis, shape in enumerate(self.noise_shape)]
#         return tuple(noise_shape)
#
#     def call(self, inputs, training=None):
#         if 0. < K.get_value(self.rate) < 1.:
#             noise_shape = self._get_noise_shape(inputs)
#
#             def dropped_inputs():
#                 return K.dropout(inputs, self.rate, noise_shape,
#                                  seed=self.seed)
#
#             return K.in_train_phase(dropped_inputs, inputs,
#                                     training=training)
#         return inputs
#
#     def get_config(self):
#         config = {'rate': K.get_value(self.rate),
#                   'noise_shape': self.noise_shape,
#                   'seed': self.seed}
#         base_config = super(MyDropout, self).get_config()
#         return dict(list(base_config.items()) + list(config.items()))
#
#     def compute_output_shape(self, input_shape):
#         return input_shape
#
#
# class DropoutReducer(tf.keras.callbacks.Callback):
#     def __init__(self, patience=0, reduce_rate=0.5, verbose=1,
#                  monitor='val_loss', **kwargs):
#         super(DropoutReducer, self).__init__(**kwargs)
#         self.patience = patience
#         self.wait = 0
#         self.best_score = -1.
#         self.reduce_rate = reduce_rate
#         self.verbose = verbose
#         self.monitor = monitor
#         self.TAG = "DROPOUT REDUCER: "
#         self.callno = -1
#         self.dropout_rate = -1
#
#     def on_epoch_end(self, epoch, logs={}):
#         current_score = logs.get(self.monitor)
#         if self.verbose == 2:
#             print(self.TAG + "---Current score: {:.4f} vs best score is:{:.4f}".format(current_score, self.best_score))
#         self.callno += 1
#         if self.callno == 0:
#             self.best_score = current_score
#         elif current_score < self.best_score:
#             self.best_score = current_score
#             self.wait = 0
#         else:
#             if self.wait >= self.patience:
#                 if self.verbose:
#                     print(self.TAG + '---Reducing Dropout Rate...')
#                 found_layers = 0
#                 for layer in self.model.layers:
#
#                     # if isinstance(layer, tf.keras.layers.Layer):
#                     if isinstance(layer, MyDropout):
#                         # if self.verbose == 2:
#                         #     print(layer)
#                         # if isinstance(layer, MyDropout):
#
#                         # self.dropout_rate = self.reduce_rate * K.get_value(layer.rate)
#                         # reduce by subtraction
#                         self.dropout_rate = K.get_value(layer.rate) - self.reduce_rate
#                         K.set_value(layer.rate, self.dropout_rate)
#                         found_layers = found_layers + 1
#
#                 if self.verbose:
#                     print(self.TAG + 'Found {} Dropout layers and reduced dropout rate to {}.'.format(found_layers,
#                                                                                                       self.dropout_rate))
#                 self.wait = 0
#             else:
#                 self.wait += 1
