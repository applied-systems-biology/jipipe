from __future__ import print_function, division
import scipy
import tensorflow as tf
# from time import time
import time
import cv2

# from keras_contrib.layers.normalization import InstanceNormalization
from keras.layers import Input, Dense, Reshape, Flatten, Dropout, Concatenate
from keras.layers import BatchNormalization, Activation, ZeroPadding2D
from keras.layers.advanced_activations import LeakyReLU
from keras.layers.convolutional import UpSampling2D, Conv2D
from keras.models import Sequential, Model
from keras import optimizers
from keras.callbacks import ModelCheckpoint, History, TensorBoard
import datetime
import matplotlib.pyplot as plt
import sys
import numpy as np
import os
import random
from data_loader import DataLoader

import helper as hp



class Pix2Pix():
    def __init__(self, squared_img_size, d_name):
        # Input shape
        self.img_rows = squared_img_size
        self.img_cols = squared_img_size
        self.channels = 1
        self.img_shape = (self.img_rows, self.img_cols, self.channels)

        # Configure data loader
        self.dataset_name = d_name
        self.data_loader = DataLoader(dataset_name=self.dataset_name,
                                      img_res=(self.img_rows, self.img_cols))

        # Calculate output shape of D (PatchGAN)
        patch = int(self.img_rows / 2**4)
        self.disc_patch = (patch, patch, 1)

        # Number of filters in the first layer of G and D
        self.gf = 64
        self.df = 64

        optimizer = optimizers.Adam(0.0002, 0.5)

        # Build and compile the discriminator
        self.discriminator = self.build_discriminator()
        self.discriminator.compile(loss='mse',
            optimizer=optimizer,
            metrics=['accuracy'])

        #-------------------------
        # Construct Computational
        #   Graph of Generator
        #-------------------------

        # Build the generator
        self.generator = self.build_generator()

        # Input images and their conditioning images
        img_A = Input(shape=self.img_shape)
        img_B = Input(shape=self.img_shape)

        # By conditioning on B generate a fake version of A
        fake_A = self.generator(img_B)

        # For the combined model we will only train the generator
        self.discriminator.trainable = False

        # Discriminators determines validity of translated images / condition pairs
        valid = self.discriminator([fake_A, img_B])

        # Save the model weights after each epoch if the validation loss decreased
        p = time.strftime("%Y-%m-%d_%H_%M_%S")
        self.checkpointer = ModelCheckpoint(filepath="logs/{}_CP".format(p), verbose=1, #filepath="logs/{}.base".format(p), verbose=1,
                                            save_best_only=True, mode='min')

        self.tensorboard = TensorBoard(log_dir="logs/{}".format(p), histogram_freq=0, batch_size=8,
            write_graph=False, write_grads=True, write_images=False, embeddings_freq=0,
            embeddings_layer_names=None, embeddings_metadata=None)

        self.combined = Model(inputs=[img_A, img_B], outputs=[valid, fake_A], name='combined')
        self.combined.compile(loss=['mse', 'mae'],
                              loss_weights=[1, 100],
                              optimizer=optimizer)

        print('finish Pix2Pix __init__')

    def build_generator(self):
        """U-Net Generator"""

        def conv2d(layer_input, filters, f_size=4, bn=True):
            """Layers used during downsampling"""
            d = Conv2D(filters, kernel_size=f_size, strides=2, padding='same')(layer_input)
            d = LeakyReLU(alpha=0.2)(d)
            if bn:
                d = BatchNormalization(momentum=0.8)(d)
            return d

        def deconv2d(layer_input, skip_input, filters, f_size=4, dropout_rate=0):
            """Layers used during upsampling"""
            u = UpSampling2D(size=2)(layer_input)
            u = Conv2D(filters, kernel_size=f_size, strides=1, padding='same', activation='relu')(u)
            if dropout_rate:
                u = Dropout(dropout_rate)(u)
            u = BatchNormalization(momentum=0.8)(u)
            u = Concatenate()([u, skip_input])
            return u

        # Image input
        d0 = Input(shape=self.img_shape)

        # Downsampling
        d1 = conv2d(d0, self.gf, bn=False)
        d2 = conv2d(d1, self.gf*2)
        d3 = conv2d(d2, self.gf*4)
        d4 = conv2d(d3, self.gf*8)
        d5 = conv2d(d4, self.gf*8)
        d6 = conv2d(d5, self.gf*8)
        d7 = conv2d(d6, self.gf*8)

        # Upsampling
        u1 = deconv2d(d7, d6, self.gf*8)
        u2 = deconv2d(u1, d5, self.gf*8)
        u3 = deconv2d(u2, d4, self.gf*8)
        u4 = deconv2d(u3, d3, self.gf*4)
        u5 = deconv2d(u4, d2, self.gf*2)
        u6 = deconv2d(u5, d1, self.gf)

        u7 = UpSampling2D(size=2)(u6)
        output_img = Conv2D(self.channels, kernel_size=4, strides=1, padding='same', activation='tanh')(u7)

        return Model(d0, output_img)

    def build_discriminator(self):

        def d_layer(layer_input, filters, f_size=4, bn=True):
            """Discriminator layer"""
            d = Conv2D(filters, kernel_size=f_size, strides=2, padding='same')(layer_input)
            d = LeakyReLU(alpha=0.2)(d)
            if bn:
                d = BatchNormalization(momentum=0.8)(d)
            return d

        img_A = Input(shape=self.img_shape)
        img_B = Input(shape=self.img_shape)

        # Concatenate image and conditioning image by channels to produce input
        combined_imgs = Concatenate(axis=-1)([img_A, img_B])

        d1 = d_layer(combined_imgs, self.df, bn=False)
        d2 = d_layer(d1, self.df*2)
        d3 = d_layer(d2, self.df*4)
        d4 = d_layer(d3, self.df*8)

        validity = Conv2D(1, kernel_size=4, strides=1, padding='same')(d4)

        return Model([img_A, img_B], validity)

    def train(self, epochs, batch_size=1, sample_interval=50):
        start_time = datetime.datetime.now()

        # Adversarial loss ground truths
        valid = np.ones((batch_size,) + self.disc_patch)
        fake = np.zeros((batch_size,) + self.disc_patch)

        for epoch in range(epochs):
            for batch_i, (imgs_A, imgs_B) in enumerate(self.data_loader.load_batch(batch_size, k_size=9)):
                # reshape images
                imgs_A, imgs_B = np.expand_dims(imgs_A, axis=3), np.expand_dims(imgs_B, axis=3)

                # ---------------------
                #  Train Discriminator
                # ---------------------

                # Condition on B and generate a translated version
                fake_A = self.generator.predict(imgs_B)

                # Train the discriminators (original images = real / generated = Fake)
                d_loss_real = self.discriminator.train_on_batch([imgs_A, imgs_B], valid)
                d_loss_fake = self.discriminator.train_on_batch([fake_A, imgs_B], fake)
                d_loss = 0.5 * np.add(d_loss_real, d_loss_fake)

                # -----------------
                #  Train Generator
                # -----------------

                # Fit the model
                g_loss = self.combined.fit([imgs_A, imgs_B], [valid, imgs_A],
                                            validation_split=0.1,
                                            verbose=0,
                                            epochs=1,
                                            batch_size=batch_size,
                                            # callbacks=[self.tensorboard])#, self.checkpointer])
                                            callbacks=[self.tensorboard, self.checkpointer])
                g_loss = g_loss.history['loss']

                # Train the generators (alternative way for train the combined model)
                # g_loss = self.combined.train_on_batch([imgs_A, imgs_B], [valid, imgs_A])

                elapsed_time = datetime.datetime.now() - start_time
                # Plot the progress
                print ("[Epoch %d/%d] [Batch %d/%d] [D loss: %f, acc: %3d%%] [G loss: %f] time: %s" % (epoch, epochs-1,
                                                                    batch_i, self.data_loader.n_batches,
                                                                    d_loss[0], 100*d_loss[1],
                                                                    g_loss[0],
                                                                    elapsed_time))

                # If at save interval => save generated image samples
                if batch_i % sample_interval == 0:
                    self.sample_images(epoch, batch_i)

        time_elapsed = datetime.datetime.now() - start_time
        print('\nFinish training in (hh:mm:ss.ms) {}'.format(time_elapsed))
        return self.discriminator, self.generator, self.combined

    def sample_images(self, epoch, batch_i):
        os.makedirs('images/%s' % self.dataset_name, exist_ok=True)
        r, c = 3, 3

        imgs_A, imgs_B = self.data_loader.load_data(batch_size=3, is_testing=True, k_size=9)
        imgs_A, imgs_B = np.expand_dims(imgs_A, axis=3), np.expand_dims(imgs_B, axis=3)

        fake_A = self.generator.predict(imgs_B)

        gen_imgs = np.concatenate([imgs_B, fake_A, imgs_A])

        # Rescale images 0 - 1
        gen_imgs = 0.5 * gen_imgs + 0.5
        gen_imgs = np.squeeze(gen_imgs, axis=3)

        # save unique images
        save_unique_image = False

        titles = ['Condition', 'Generated', 'Original']
        cnt = 0
        if save_unique_image:
            for i in range(r):
                for _ in range(c):
                    hp.image_saver(gen_imgs[cnt], titles[i], self.dataset_name, epoch, batch_i)
                    cnt += 1
        else:
            fig, axs = plt.subplots(r, c, figsize=(20,20))
            for i in range(r):
                for j in range(c):
                    axs[i,j].imshow(gen_imgs[cnt], cmap='gray')
                    axs[i,j].set_title(titles[i])
                    axs[i,j].axis('off')
                    cnt += 1

            fig.savefig("images/%s/%d_%d.png" % (self.dataset_name, epoch, batch_i))
            plt.close()
            # img_tmp = cv2.imread("images/%s/%d_%d.png" % (self.dataset_name, epoch, batch_i), 0)
            # tf.summary.image('result_images', img_tmp)
