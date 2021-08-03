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
from matplotlib import pyplot as plt
import tensorflow as tf


# from IPython.display import clear_output

def get_callbacks(input_model_path, log_dir):
    """
    Provide all tensorflow and custom defined callbacks dependent on a verbose level.
    Args:
        input_model_path: save path for the model checkpoint callback
        log_dir: save path for the logging directory

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

    print(f'[Get callbacks] get number of callbacks: <{len(callbacks)}>')

    return callbacks


def display(display_list):
    plt.figure(figsize=(15, 15))

    title = ['Input Image', 'True Mask', 'Predicted Mask']

    for i in range(len(display_list)):
        plt.subplot(1, len(display_list), i + 1)
        plt.title(title[i])
        plt.imshow(tf.keras.preprocessing.image.array_to_img(display_list[i]))
        plt.axis('off')
    plt.show()


def create_mask(pred_mask):
    pred_mask = tf.argmax(pred_mask, axis=-1)
    pred_mask = pred_mask[..., tf.newaxis]
    return pred_mask[0]


def show_predictions(self, dataset=None, num=1):
    if dataset:
        for image, mask in dataset.take(num):
            pred_mask = self.model.predict(image)
            display([image[0], mask[0], create_mask(pred_mask)])
    else:

        # TODO: Zugang auf validierunsBilder und diese zu sample_image, sample_mask... zuordnen
        # nicht so :
        # train = dataset['train'].map(load_image_train, num_parallel_calls=tf.data.AUTOTUNE)
        # for image, mask in train.take(1):
        #     sample_image, sample_mask = image, mask
        # display([sample_image, sample_mask])

        display([sample_image, sample_mask, create_mask(self.model.predict(sample_image[tf.newaxis, ...]))])


class DisplayPrediction(tf.keras.callbacks.Callback):
    """
    Display an input with its according label images and prediction.
    """

    def on_epoch_end(self, epoch, logs=None):
        # clear_output(wait=True)
        show_predictions()
        print('\nSample Prediction after epoch {}\n'.format(epoch + 1))


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


### TODO: confusion matrix callback


### custom callback for store confusion matrix (for classification purpose)
class ConfusionMatrixCallback(tf.keras.callbacks.Callback):

    def __init__(self, log_dir, classes):
        self.log_dir = log_dir
        self.file_writer = tf.summary.FileWriter(self.log_dir + '/cm')

        # get label for classes from config file ['classes']
        self.classes = []
        for key, value in classes.items():
            if 'Fusarium' in value:
                continue
            self.classes.append(value)

    def on_epoch_end(self, epoch, logs=None):

        # Use the model to predict the values from the validation dataset.
        test_pred = self.model.predict(self.validation_data[0]).argmax(axis=1)
        test_label = self.validation_data[1].argmax(axis=1)

        # with tf.Session():
        #     con_mat = tf.confusion_matrix(labels=test_label, predictions=test_pred).eval()
        # # NO mirror of dataframe in original confusion_matrix
        # con_mat_orig_csv = pd.DataFrame(con_mat, index = self.classes, columns = self.classes)
        # con_mat_orig_csv.to_csv(os.path.join(self.log_dir, 'confusion_matrix_orig.csv'))
        #
        # con_mat_norm = np.around(con_mat.astype('float') / con_mat.sum(axis=1)[:, np.newaxis], decimals=5)
        #
        # con_mat_df = pd.DataFrame(con_mat_norm, index=self.classes, columns=self.classes)
        # # save confusion matrix as csv
        # con_mat_df.to_csv(os.path.join(self.log_dir, 'confusion_matrix_norm.csv'))
        #
        # # mirror the dataframe
        # con_mat_df = con_mat_df.iloc[::-1,::-1]
        #
        # figure = plt.figure(figsize=(8, 8))
        # sns.heatmap(con_mat_df, annot=True, cmap=plt.cm.Blues, vmin=0, vmax=1, annot_kws={"fontsize":20})
        # sns.set(font_scale=1.0)
        # plt.tight_layout()
        # plt.ylabel('True label')
        # plt.xlabel('Predicted label')
        #
        # buf = io.BytesIO()
        # plt.savefig(buf, format='png')
        #
        # # convert figure to numpy array
        # figure.canvas.draw()
        # # Now we can save it to a numpy array.
        # data = np.fromstring(figure.canvas.tostring_rgb(), dtype=np.uint8, sep='')
        # data = data.reshape(figure.canvas.get_width_height()[::-1] + (3,))
        # skimage.io.imsave(os.path.join(self.log_dir, 'confusion_matrix_norm.png'), data)
        #
        # height, width, channel = data.shape
        #
        # # show confusion_matrix every 10-th epoch
        # if epoch % 10 == 0 or epoch == self.params['epochs']:
        #     print(con_mat[::-1,::-1], self.classes[::-1])
        #     plt.show()
        #
        # plt.close(figure)
        # image_string = buf.getvalue()
        # buf.close()
        #
        # # Log the confusion matrix as an image summary.
        # image = tf.Summary.Image(height=height, width=width, colorspace=channel, encoded_image_string=image_string)
        #
        # summary = tf.Summary(value=[tf.Summary.Value(tag="Confusion_Matrix", image=image)])
        # self.file_writer.add_summary(summary, epoch)
        # self.file_writer.close()


### TODO dropout reducer implementieren:

class MyDropout(tf.keras.layers.Layer):
    # @interfaces.legacy_dropout_support
    def __init__(self, rate, noise_shape=None, seed=None, **kwargs):
        super(MyDropout, self).__init__(**kwargs)
        self.rate = K.variable(min(1., max(0., rate)))
        self.noise_shape = noise_shape
        self.seed = seed
        self.supports_masking = True

    def _get_noise_shape(self, inputs):
        if self.noise_shape is None:
            return self.noise_shape

        symbolic_shape = K.shape(inputs)
        noise_shape = [symbolic_shape[axis] if shape is None else shape
                       for axis, shape in enumerate(self.noise_shape)]
        return tuple(noise_shape)

    def call(self, inputs, training=None):
        if 0. < K.get_value(self.rate) < 1.:
            noise_shape = self._get_noise_shape(inputs)

            def dropped_inputs():
                return K.dropout(inputs, self.rate, noise_shape,
                                 seed=self.seed)

            return K.in_train_phase(dropped_inputs, inputs,
                                    training=training)
        return inputs

    def get_config(self):
        config = {'rate': K.get_value(self.rate),
                  'noise_shape': self.noise_shape,
                  'seed': self.seed}
        base_config = super(MyDropout, self).get_config()
        return dict(list(base_config.items()) + list(config.items()))

    def compute_output_shape(self, input_shape):
        return input_shape


class DropoutReducer(tf.keras.callbacks.Callback):
    def __init__(self, patience=0, reduce_rate=0.5, verbose=1,
                 monitor='val_loss', **kwargs):
        super(DropoutReducer, self).__init__(**kwargs)
        self.patience = patience
        self.wait = 0
        self.best_score = -1.
        self.reduce_rate = reduce_rate
        self.verbose = verbose
        self.monitor = monitor
        self.TAG = "DROPOUT REDUCER: "
        self.callno = -1
        self.dropout_rate = -1

    def on_epoch_end(self, epoch, logs={}):
        current_score = logs.get(self.monitor)
        if self.verbose == 2:
            print(self.TAG + "---Current score: {:.4f} vs best score is:{:.4f}".format(current_score, self.best_score))
        self.callno += 1
        if self.callno == 0:
            self.best_score = current_score
        elif current_score < self.best_score:
            self.best_score = current_score
            self.wait = 0
        else:
            if self.wait >= self.patience:
                if self.verbose:
                    print(self.TAG + '---Reducing Dropout Rate...')
                found_layers = 0
                for layer in self.model.layers:

                    # if isinstance(layer, tf.keras.layers.Layer):
                    if isinstance(layer, MyDropout):
                        # if self.verbose == 2:
                        #     print(layer)
                        # if isinstance(layer, MyDropout):

                        # self.dropout_rate = self.reduce_rate * K.get_value(layer.rate)
                        # reduce by subtraction
                        self.dropout_rate = K.get_value(layer.rate) - self.reduce_rate
                        K.set_value(layer.rate, self.dropout_rate)
                        found_layers = found_layers + 1

                if self.verbose:
                    print(self.TAG + 'Found {} Dropout layers and reduced dropout rate to {}.'.format(found_layers,
                                                                                                      self.dropout_rate))
                self.wait = 0
            else:
                self.wait += 1
