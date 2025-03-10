/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg;

import ij.ImagePlus;

import java.util.Stack;

/**
 * Customized copy of TurboRegMask (package private)
 */
public class TurboRegMask
        implements
        Runnable {

    private final Stack<float[]> pyramid = new Stack<>();
    private float[] mask;
    private int width;
    private int height;
    private int pyramidDepth;

    /**
     * Converts the pixel array of the incoming <code>ImagePlus</code>
     * object into a local <code>boolean</code> array.
     *
     * @param imp <code>ImagePlus</code> object to preprocess.
     **/
    public TurboRegMask(
            final ImagePlus imp
    ) {
        width = imp.getWidth();
        height = imp.getHeight();
        int k = 0;

        mask = new float[width * height];
        if (imp.getType() == ImagePlus.GRAY8) {
            final byte[] pixels = (byte[]) imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    mask[k] = (float) pixels[k];
                }

            }
        } else if (imp.getType() == ImagePlus.GRAY16) {
            final short[] pixels = (short[]) imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    mask[k] = (float) pixels[k];
                }

            }
        } else if (imp.getType() == ImagePlus.GRAY32) {
            final float[] pixels = (float[]) imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    mask[k] = pixels[k];
                }

            }
        }

    }

    /**
     * Set to <code>true</code> every pixel of the full-size mask.
     **/
    public void clearMask(
    ) {
        int k = 0;

        for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++) {
                mask[k++] = 1.0F;
            }

        }

    }

    /**
     * Return the full-size mask array.
     **/
    public float[] getMask(
    ) {
        return (mask);
    }

    public Stack<float[]> getPyramid(
    ) {
        return (pyramid);
    }

    /**
     * Start the mask precomputations, which are interruptible.
     **/
    public void run(
    ) {
        buildPyramid();
    }

    /**
     * Set the depth up to which the pyramids should be computed.
     *
     * @see TurboRegMask#getPyramid()
     **/
    public void setPyramidDepth(
            final int pyramidDepth
    ) {
        this.pyramidDepth = pyramidDepth;
    }

    private void buildPyramid(
    ) {
        int fullWidth;
        int fullHeight;
        float[] fullMask = mask;
        int halfWidth = width;
        int halfHeight = height;
        for (int depth = 1; ((depth < pyramidDepth));
             depth++) {
            fullWidth = halfWidth;
            fullHeight = halfHeight;
            halfWidth /= 2;
            halfHeight /= 2;
            final float[] halfMask = getHalfMask2D(fullMask, fullWidth, fullHeight);
            pyramid.push(halfMask);
            fullMask = halfMask;
        }
    }

    private float[] getHalfMask2D(
            final float[] fullMask,
            final int fullWidth,
            final int fullHeight
    ) {
        final int halfWidth = fullWidth / 2;
        final int halfHeight = fullHeight / 2;
        final boolean oddWidth = ((2 * halfWidth) != fullWidth);
        int workload = 2 * halfHeight;
        final float[] halfMask = new float[halfWidth * halfHeight];
        int k = 0;
        for (int y = 0; ((y < halfHeight)); y++) {
            for (int x = 0; (x < halfWidth); x++) {
                halfMask[k++] = 0.0F;
            }

            workload--;
        }
        k = 0;
        int n = 0;
        for (int y = 0; ((y < (halfHeight - 1))); y++) {
            for (int x = 0; (x < (halfWidth - 1)); x++) {
                halfMask[k] += Math.abs(fullMask[n++]);
                halfMask[k] += Math.abs(fullMask[n]);
                halfMask[++k] += Math.abs(fullMask[n++]);
            }
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k++] += Math.abs(fullMask[n++]);
            if (oddWidth) {
                n++;
            }
            for (int x = 0; (x < (halfWidth - 1)); x++) {
                halfMask[k - halfWidth] += Math.abs(fullMask[n]);
                halfMask[k] += Math.abs(fullMask[n++]);
                halfMask[k - halfWidth] += Math.abs(fullMask[n]);
                halfMask[k - halfWidth + 1] += Math.abs(fullMask[n]);
                halfMask[k] += Math.abs(fullMask[n]);
                halfMask[++k] += Math.abs(fullMask[n++]);
            }
            halfMask[k - halfWidth] += Math.abs(fullMask[n]);
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k - halfWidth] += Math.abs(fullMask[n]);
            halfMask[k++] += Math.abs(fullMask[n++]);
            if (oddWidth) {
                n++;
            }
            k -= halfWidth;

            workload--;
        }
        for (int x = 0; (x < (halfWidth - 1)); x++) {
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k] += Math.abs(fullMask[n]);
            halfMask[++k] += Math.abs(fullMask[n++]);
        }
        halfMask[k] += Math.abs(fullMask[n++]);
        halfMask[k++] += Math.abs(fullMask[n++]);
        if (oddWidth) {
            n++;
        }
        k -= halfWidth;
        for (int x = 0; (x < (halfWidth - 1)); x++) {
            halfMask[k] += Math.abs(fullMask[n++]);
            halfMask[k] += Math.abs(fullMask[n]);
            halfMask[++k] += Math.abs(fullMask[n++]);
        }
        halfMask[k] += Math.abs(fullMask[n++]);
        halfMask[k] += Math.abs(fullMask[n]);

        workload--;


        return (halfMask);
    }

} 