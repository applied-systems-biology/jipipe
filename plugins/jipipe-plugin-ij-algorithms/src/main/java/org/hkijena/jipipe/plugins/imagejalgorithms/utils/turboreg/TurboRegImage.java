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
 * Customized copy of TurboRegImage (package private)
 */
public class TurboRegImage
        implements
        Runnable {

    private final Stack<Object> pyramid = new Stack<>();
    private final int width;
    private final int height;
    private final boolean isTarget;
    private float[] image;
    private float[] coefficient;
    private float[] xGradient;
    private float[] yGradient;
    private int pyramidDepth;
    private TurboRegTransformationType transformation;


    /**
     * Converts the pixel array of the incoming <code>ImagePlus</code>
     * object into a local <code>float</code> array.
     *
     * @param imp            <code>ImagePlus</code> object to preprocess.
     * @param transformation Transformation code.
     * @param isTarget       Tags the current object as a target or source image.
     **/
    public TurboRegImage(
            final ImagePlus imp,
            final TurboRegTransformationType transformation,
            final boolean isTarget
    ) {
        this.transformation = transformation;
        this.isTarget = isTarget;
        width = imp.getWidth();
        height = imp.getHeight();
        int k = 0;

        if (imp.getType() == ImagePlus.GRAY8) {
            image = new float[width * height];
            final byte[] pixels = (byte[]) imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    image[k] = (float) (pixels[k] & 0xFF);
                }

            }
        } else if (imp.getType() == ImagePlus.GRAY16) {
            image = new float[width * height];
            final short[] pixels = (short[]) imp.getProcessor().getPixels();
            for (int y = 0; (y < height); y++) {
                for (int x = 0; (x < width); x++, k++) {
                    if (pixels[k] < (short) 0) {
                        image[k] = (float) pixels[k] + 65536.0F;
                    } else {
                        image[k] = pixels[k];
                    }
                }

            }
        } else if (imp.getType() == ImagePlus.GRAY32) {
            image = (float[]) imp.getProcessor().getPixels();
        }

    }

    /**
     * Return the B-spline coefficients of the full-size image.
     **/
    public float[] getCoefficient(
    ) {
        return (coefficient);
    }

    /**
     * Return the full-size image height.
     **/
    public int getHeight(
    ) {
        return (height);
    }

    /**
     * Return the full-size image array.
     **/
    public float[] getImage(
    ) {
        return (image);
    }

    /**
     * Return the image pyramid as a <code>Stack</code> object. The organization
     * of the stack depends on whether the <code>TurboRegImage</code>
     * object corresponds to the target or the source image, and on the
     * transformation (ML* = {<code>TRANSLATION</code>,<code>RIGID_BODY</code>,
     * <code>SCALED_ROTATION</code>, <code>AFFINE</code>} vs.
     * ML = {<code>BILINEAR<code>}). A single pyramid level consists of
     * <p>
     * <table border="1">
     * <tr><th><code>isTarget</code></th>
     * <th>ML*</th>
     * <th>ML</th></tr>
     * <tr><td>true</td>
     * <td>width<br>height<br>B-spline coefficients</td>
     * <td>width<br>height<br>samples</td></tr>
     * <tr><td>false</td>
     * <td>width<br>height<br>samples<br>horizontal gradients<br>
     * vertical gradients</td>
     * <td>width<br>height<br>B-spline coefficients</td></tr>
     * </table>
     **/
    public Stack<Object> getPyramid(
    ) {
        return (pyramid);
    }

    /**
     * Return the depth of the image pyramid. A depth <code>1</code> means
     * that one coarse resolution level is present in the stack. The
     * full-size level is not placed on the stack.
     **/
    public int getPyramidDepth(
    ) {
        return (pyramidDepth);
    }

    /**
     * Sets the depth up to which the pyramids should be computed.
     *
     * @see TurboRegImage#getImage()
     **/
    public void setPyramidDepth(
            final int pyramidDepth
    ) {
        this.pyramidDepth = pyramidDepth;
    }

    /**
     * Return the full-size image width.
     **/
    public int getWidth(
    ) {
        return (width);
    }

    /**
     * Return the full-size horizontal gradient of the image, if available.
     *
     * @see TurboRegImage#getPyramid()
     **/
    public float[] getXGradient(
    ) {
        return (xGradient);
    }

    /**
     * Return the full-size vertical gradient of the image, if available.
     *
     * @see TurboRegImage#getImage()
     **/
    public float[] getYGradient(
    ) {
        return (yGradient);
    }

    /**
     * Start the image precomputations. The computation of the B-spline
     * coefficients of the full-size image is not interruptible; all other
     * methods are.
     **/
    public void run(
    ) {
        coefficient = getBasicFromCardinal2D();
        switch (transformation) {
            case GenericTransformation: {
                break;
            }
            case Translation:
            case RigidBody:
            case ScaledRotation:
            case Affine: {
                if (isTarget) {
                    buildCoefficientPyramid();
                } else {
                    imageToXYGradient2D();
                    buildImageAndGradientPyramid();
                }
                break;
            }
            case Bilinear: {
                if (isTarget) {
                    buildImagePyramid();
                } else {
                    buildCoefficientPyramid();
                }
                break;
            }
        }
    }

    /**
     * Set or modify the transformation.
     **/
    public void setTransformation(
            final TurboRegTransformationType transformation
    ) {
        this.transformation = transformation;
    }

/*....................................................................
	Private methods
....................................................................*/

    /*------------------------------------------------------------------*/
    private void antiSymmetricFirMirrorOffBounds1D(
            final double[] h,
            final double[] c,
            final double[] s
    ) {
        if (2 <= c.length) {
            s[0] = h[1] * (c[1] - c[0]);
            for (int i = 1; (i < (s.length - 1)); i++) {
                s[i] = h[1] * (c[i + 1] - c[i - 1]);
            }
            s[s.length - 1] = h[1] * (c[c.length - 1] - c[c.length - 2]);
        } else {
            s[0] = 0.0;
        }
    }

    /*------------------------------------------------------------------*/
    private void basicToCardinal2D(
            final float[] basic,
            final float[] cardinal,
            final int width,
            final int height,
            final int degree
    ) {
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];
        final double[] hData = new double[width];
        final double[] vData = new double[height];
        double[] h = null;
        switch (degree) {
            case 3: {
                h = new double[2];
                h[0] = 2.0 / 3.0;
                h[1] = 1.0 / 6.0;
                break;
            }
            case 7: {
                h = new double[4];
                h[0] = 151.0 / 315.0;
                h[1] = 397.0 / 1680.0;
                h[2] = 1.0 / 42.0;
                h[3] = 1.0 / 5040.0;
                break;
            }
            default: {
                h = new double[1];
                h[0] = 1.0;
            }
        }
        for (int y = 0; ((y < height)); y++) {
            extractRow(basic, y, hLine);
            symmetricFirMirrorOffBounds1D(h, hLine, hData);
            putRow(cardinal, y, hData);
        }
        for (int x = 0; ((x < width)); x++) {
            extractColumn(cardinal, width, x, vLine);
            symmetricFirMirrorOffBounds1D(h, vLine, vData);
            putColumn(cardinal, width, x, vData);
        }
    }

    /*------------------------------------------------------------------*/
    private void buildCoefficientPyramid(
    ) {
        int fullWidth;
        int fullHeight;
        float[] fullDual = new float[width * height];
        int halfWidth = width;
        int halfHeight = height;
        if (1 < pyramidDepth) {
            basicToCardinal2D(coefficient, fullDual, width, height, 7);
        }
        for (int depth = 1; ((depth < pyramidDepth));
             depth++) {
            fullWidth = halfWidth;
            fullHeight = halfHeight;
            halfWidth /= 2;
            halfHeight /= 2;
            final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
            final float[] halfCoefficient = getBasicFromCardinal2D(
                    halfDual, halfWidth, halfHeight, 7);
            pyramid.push(halfCoefficient);
            pyramid.push(halfHeight);
            pyramid.push(halfWidth);
            fullDual = halfDual;
        }
    }

    /*------------------------------------------------------------------*/
    private void buildImageAndGradientPyramid(
    ) {
        int fullWidth;
        int fullHeight;
        float[] fullDual = new float[width * height];
        int halfWidth = width;
        int halfHeight = height;
        if (1 < pyramidDepth) {
            cardinalToDual2D(image, fullDual, width, height, 3);
        }
        for (int depth = 1; ((depth < pyramidDepth));
             depth++) {
            fullWidth = halfWidth;
            fullHeight = halfHeight;
            halfWidth /= 2;
            halfHeight /= 2;
            final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
            final float[] halfImage = getBasicFromCardinal2D(
                    halfDual, halfWidth, halfHeight, 7);
            final float[] halfXGradient = new float[halfWidth * halfHeight];
            final float[] halfYGradient = new float[halfWidth * halfHeight];
            coefficientToXYGradient2D(halfImage, halfXGradient, halfYGradient,
                    halfWidth, halfHeight);
            basicToCardinal2D(halfImage, halfImage, halfWidth, halfHeight, 3);
            pyramid.push(halfYGradient);
            pyramid.push(halfXGradient);
            pyramid.push(halfImage);
            pyramid.push(halfHeight);
            pyramid.push(halfWidth);
            fullDual = halfDual;
        }
    }

    /*------------------------------------------------------------------*/
    private void buildImagePyramid(
    ) {
        int fullWidth;
        int fullHeight;
        float[] fullDual = new float[width * height];
        int halfWidth = width;
        int halfHeight = height;
        if (1 < pyramidDepth) {
            cardinalToDual2D(image, fullDual, width, height, 3);
        }
        for (int depth = 1; ((depth < pyramidDepth));
             depth++) {
            fullWidth = halfWidth;
            fullHeight = halfHeight;
            halfWidth /= 2;
            halfHeight /= 2;
            final float[] halfDual = getHalfDual2D(fullDual, fullWidth, fullHeight);
            final float[] halfImage = new float[halfWidth * halfHeight];
            dualToCardinal2D(halfDual, halfImage, halfWidth, halfHeight, 3);
            pyramid.push(halfImage);
            pyramid.push(halfHeight);
            pyramid.push(halfWidth);
            fullDual = halfDual;
        }
    }

    /*------------------------------------------------------------------*/
    private void cardinalToDual2D(
            final float[] cardinal,
            final float[] dual,
            final int width,
            final int height,
            final int degree
    ) {
        basicToCardinal2D(getBasicFromCardinal2D(cardinal, width, height, degree),
                dual, width, height, 2 * degree + 1);
    }

    /*------------------------------------------------------------------*/
    private void coefficientToGradient1D(
            final double[] c
    ) {
        final double[] h = {0.0, 1.0 / 2.0};
        final double[] s = new double[c.length];
        antiSymmetricFirMirrorOffBounds1D(h, c, s);
        System.arraycopy(s, 0, c, 0, s.length);
    }

    /*------------------------------------------------------------------*/
    private void coefficientToSamples1D(
            final double[] c
    ) {
        final double[] h = {2.0 / 3.0, 1.0 / 6.0};
        final double[] s = new double[c.length];
        symmetricFirMirrorOffBounds1D(h, c, s);
        System.arraycopy(s, 0, c, 0, s.length);
    }

    /*------------------------------------------------------------------*/
    private void coefficientToXYGradient2D(
            final float[] basic,
            final float[] xGradient,
            final float[] yGradient,
            final int width,
            final int height
    ) {
        final double[] hLine = new double[width];
        final double[] hData = new double[width];
        final double[] vLine = new double[height];
        int workload = 2 * (width + height);

        for (int y = 0; ((y < height)); y++) {
            extractRow(basic, y, hLine);
            System.arraycopy(hLine, 0, hData, 0, width);
            coefficientToGradient1D(hLine);

            workload--;
            coefficientToSamples1D(hData);
            putRow(xGradient, y, hLine);
            putRow(yGradient, y, hData);

            workload--;
        }
        for (int x = 0; ((x < width)); x++) {
            extractColumn(xGradient, width, x, vLine);
            coefficientToSamples1D(vLine);
            putColumn(xGradient, width, x, vLine);

            workload--;
            extractColumn(yGradient, width, x, vLine);
            coefficientToGradient1D(vLine);
            putColumn(yGradient, width, x, vLine);

            workload--;
        }


    }

    /*------------------------------------------------------------------*/
    private void dualToCardinal2D(
            final float[] dual,
            final float[] cardinal,
            final int width,
            final int height,
            final int degree
    ) {
        basicToCardinal2D(getBasicFromCardinal2D(dual, width, height,
                2 * degree + 1), cardinal, width, height, degree);
    }

    /*------------------------------------------------------------------*/
    private void extractColumn(
            final float[] array,
            final int width,
            int x,
            final double[] column
    ) {
        for (int i = 0; (i < column.length); i++) {
            column[i] = array[x];
            x += width;
        }
    }

    /*------------------------------------------------------------------*/
    private void extractRow(
            final float[] array,
            int y,
            final double[] row
    ) {
        y *= row.length;
        for (int i = 0; (i < row.length); i++) {
            row[i] = array[y++];
        }
    }

    /*------------------------------------------------------------------*/
    private float[] getBasicFromCardinal2D(
    ) {
        final float[] basic = new float[width * height];
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];

        for (int y = 0; (y < height); y++) {
            extractRow(image, y, hLine);
            samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
            putRow(basic, y, hLine);

        }
        for (int x = 0; (x < width); x++) {
            extractColumn(basic, width, x, vLine);
            samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
            putColumn(basic, width, x, vLine);

        }

        return (basic);
    }

    /*------------------------------------------------------------------*/
    private float[] getBasicFromCardinal2D(
            final float[] cardinal,
            final int width,
            final int height,
            final int degree
    ) {
        final float[] basic = new float[width * height];
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];
        int workload = width + height;

        for (int y = 0; ((y < height)); y++) {
            extractRow(cardinal, y, hLine);
            samplesToInterpolationCoefficient1D(hLine, degree, 0.0);
            putRow(basic, y, hLine);

            workload--;
        }
        for (int x = 0; ((x < width)); x++) {
            extractColumn(basic, width, x, vLine);
            samplesToInterpolationCoefficient1D(vLine, degree, 0.0);
            putColumn(basic, width, x, vLine);

            workload--;
        }


        return (basic);
    }

    /*------------------------------------------------------------------*/
    private float[] getHalfDual2D(
            final float[] fullDual,
            final int fullWidth,
            final int fullHeight
    ) {
        final int halfWidth = fullWidth / 2;
        final int halfHeight = fullHeight / 2;
        final double[] hLine = new double[fullWidth];
        final double[] hData = new double[halfWidth];
        final double[] vLine = new double[fullHeight];
        final double[] vData = new double[halfHeight];
        final float[] demiDual = new float[halfWidth * fullHeight];
        final float[] halfDual = new float[halfWidth * halfHeight];
        int workload = halfWidth + fullHeight;

        for (int y = 0; ((y < fullHeight)); y++) {
            extractRow(fullDual, y, hLine);
            reduceDual1D(hLine, hData);
            putRow(demiDual, y, hData);

            workload--;
        }
        for (int x = 0; ((x < halfWidth)); x++) {
            extractColumn(demiDual, halfWidth, x, vLine);
            reduceDual1D(vLine, vData);
            putColumn(halfDual, halfWidth, x, vData);

            workload--;
        }


        return (halfDual);
    }

    /*------------------------------------------------------------------*/
    private double getInitialAntiCausalCoefficientMirrorOffBounds(
            final double[] c,
            final double z,
            final double tolerance
    ) {
        return (z * c[c.length - 1] / (z - 1.0));
    }

    /*------------------------------------------------------------------*/
    private double getInitialCausalCoefficientMirrorOffBounds(
            final double[] c,
            final double z,
            final double tolerance
    ) {
        double z1 = z;
        double zn = Math.pow(z, c.length);
        double sum = (1.0 + z) * (c[0] + zn * c[c.length - 1]);
        int horizon = c.length;
        if (0.0 < tolerance) {
            horizon = 2 + (int) (Math.log(tolerance) / Math.log(Math.abs(z)));
            horizon = Math.min(horizon, c.length);
        }
        zn = zn * zn;
        for (int n = 1; (n < (horizon - 1)); n++) {
            z1 = z1 * z;
            zn = zn / z;
            sum = sum + (z1 + zn) * c[n];
        }
        return (sum / (1.0 - Math.pow(z, 2 * c.length)));
    }

    /*------------------------------------------------------------------*/
    private void imageToXYGradient2D(
    ) {
        final double[] hLine = new double[width];
        final double[] vLine = new double[height];
        xGradient = new float[width * height];
        yGradient = new float[width * height];
        int workload = width + height;

        for (int y = 0; ((y < height)); y++) {
            extractRow(image, y, hLine);
            samplesToInterpolationCoefficient1D(hLine, 3, 0.0);
            coefficientToGradient1D(hLine);
            putRow(xGradient, y, hLine);

            workload--;
        }
        for (int x = 0; ((x < width)); x++) {
            extractColumn(image, width, x, vLine);
            samplesToInterpolationCoefficient1D(vLine, 3, 0.0);
            coefficientToGradient1D(vLine);
            putColumn(yGradient, width, x, vLine);

            workload--;
        }


    }

    /*------------------------------------------------------------------*/
    private void putColumn(
            final float[] array,
            final int width,
            int x,
            final double[] column
    ) {
        for (int i = 0; (i < column.length); i++) {
            array[x] = (float) column[i];
            x += width;
        }
    }

    /*------------------------------------------------------------------*/
    private void putRow(
            final float[] array,
            int y,
            final double[] row
    ) {
        y *= row.length;
        for (int i = 0; (i < row.length); i++) {
            array[y++] = (float) row[i];
        }
    }

    /*------------------------------------------------------------------*/
    private void reduceDual1D(
            final double[] c,
            final double[] s
    ) {
        final double[] h = {6.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0};
        if (2 <= s.length) {
            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
            for (int i = 2, j = 1; (j < (s.length - 1)); i += 2, j++) {
                s[j] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
                        + h[2] * (c[i - 2] + c[i + 2]);
            }
            if (c.length == (2 * s.length)) {
                s[s.length - 1] = h[0] * c[c.length - 2]
                        + h[1] * (c[c.length - 3] + c[c.length - 1])
                        + h[2] * (c[c.length - 4] + c[c.length - 1]);
            } else {
                s[s.length - 1] = h[0] * c[c.length - 3]
                        + h[1] * (c[c.length - 4] + c[c.length - 2])
                        + h[2] * (c[c.length - 5] + c[c.length - 1]);
            }
        } else {
            switch (c.length) {
                case 3: {
                    s[0] = h[0] * c[0]
                            + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
                    break;
                }
                case 2: {
                    s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + 2.0 * h[2] * c[1];
                    break;
                }
            }
        }
    }

    /*------------------------------------------------------------------*/
    private void samplesToInterpolationCoefficient1D(
            final double[] c,
            final int degree,
            final double tolerance
    ) {
        double[] z = new double[0];
        double lambda = 1.0;
        switch (degree) {
            case 3: {
                z = new double[1];
                z[0] = Math.sqrt(3.0) - 2.0;
                break;
            }
            case 7: {
                z = new double[3];
                z[0] =
                        -0.5352804307964381655424037816816460718339231523426924148812;
                z[1] =
                        -0.122554615192326690515272264359357343605486549427295558490763;
                z[2] =
                        -0.0091486948096082769285930216516478534156925639545994482648003;
                break;
            }
        }
        if (c.length == 1) {
            return;
        }
        for (int k = 0; (k < z.length); k++) {
            lambda *= (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
        }
        for (int n = 0; (n < c.length); n++) {
            c[n] = c[n] * lambda;
        }
        for (int k = 0; (k < z.length); k++) {
            c[0] = getInitialCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
            for (int n = 1; (n < c.length); n++) {
                c[n] = c[n] + z[k] * c[n - 1];
            }
            c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOffBounds(
                    c, z[k], tolerance);
            for (int n = c.length - 2; (0 <= n); n--) {
                c[n] = z[k] * (c[n + 1] - c[n]);
            }
        }
    }

    /*------------------------------------------------------------------*/
    private void symmetricFirMirrorOffBounds1D(
            final double[] h,
            final double[] c,
            final double[] s
    ) {
        switch (h.length) {
            case 2: {
                if (2 <= c.length) {
                    s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]);
                    for (int i = 1; (i < (s.length - 1)); i++) {
                        s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
                    }
                    s[s.length - 1] = h[0] * c[c.length - 1]
                            + h[1] * (c[c.length - 2] + c[c.length - 1]);
                } else {
                    s[0] = (h[0] + 2.0 * h[1]) * c[0];
                }
                break;
            }
            case 4: {
                if (6 <= c.length) {
                    s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2])
                            + h[3] * (c[2] + c[3]);
                    s[1] = h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[0] + c[3])
                            + h[3] * (c[1] + c[4]);
                    s[2] = h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[4])
                            + h[3] * (c[0] + c[5]);
                    for (int i = 3; (i < (s.length - 3)); i++) {
                        s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
                                + h[2] * (c[i - 2] + c[i + 2])
                                + h[3] * (c[i - 3] + c[i + 3]);
                    }
                    s[s.length - 3] = h[0] * c[c.length - 3]
                            + h[1] * (c[c.length - 4] + c[c.length - 2])
                            + h[2] * (c[c.length - 5] + c[c.length - 1])
                            + h[3] * (c[c.length - 6] + c[c.length - 1]);
                    s[s.length - 2] = h[0] * c[c.length - 2]
                            + h[1] * (c[c.length - 3] + c[c.length - 1])
                            + h[2] * (c[c.length - 4] + c[c.length - 1])
                            + h[3] * (c[c.length - 5] + c[c.length - 2]);
                    s[s.length - 1] = h[0] * c[c.length - 1]
                            + h[1] * (c[c.length - 2] + c[c.length - 1])
                            + h[2] * (c[c.length - 3] + c[c.length - 2])
                            + h[3] * (c[c.length - 4] + c[c.length - 3]);
                } else {
                    switch (c.length) {
                        case 5: {
                            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
                                    + h[2] * (c[1] + c[2]) + h[3] * (c[2] + c[3]);
                            s[1] = h[0] * c[1] + h[1] * (c[0] + c[2])
                                    + h[2] * (c[0] + c[3]) + h[3] * (c[1] + c[4]);
                            s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
                                    + (h[2] + h[3]) * (c[0] + c[4]);
                            s[3] = h[0] * c[3] + h[1] * (c[2] + c[4])
                                    + h[2] * (c[1] + c[4]) + h[3] * (c[0] + c[3]);
                            s[4] = h[0] * c[4] + h[1] * (c[3] + c[4])
                                    + h[2] * (c[2] + c[3]) + h[3] * (c[1] + c[2]);
                            break;
                        }
                        case 4: {
                            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
                                    + h[2] * (c[1] + c[2]) + h[3] * (c[2] + c[3]);
                            s[1] = h[0] * c[1] + h[1] * (c[0] + c[2])
                                    + h[2] * (c[0] + c[3]) + h[3] * (c[1] + c[3]);
                            s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
                                    + h[2] * (c[0] + c[3]) + h[3] * (c[0] + c[2]);
                            s[3] = h[0] * c[3] + h[1] * (c[2] + c[3])
                                    + h[2] * (c[1] + c[2]) + h[3] * (c[0] + c[1]);
                            break;
                        }
                        case 3: {
                            s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
                                    + h[2] * (c[1] + c[2]) + 2.0 * h[3] * c[2];
                            s[1] = h[0] * c[1] + (h[1] + h[2]) * (c[0] + c[2])
                                    + 2.0 * h[3] * c[1];
                            s[2] = h[0] * c[2] + h[1] * (c[1] + c[2])
                                    + h[2] * (c[0] + c[1]) + 2.0 * h[3] * c[0];
                            break;
                        }
                        case 2: {
                            s[0] = (h[0] + h[1] + h[3]) * c[0]
                                    + (h[1] + 2.0 * h[2] + h[3]) * c[1];
                            s[1] = (h[0] + h[1] + h[3]) * c[1]
                                    + (h[1] + 2.0 * h[2] + h[3]) * c[0];
                            break;
                        }
                        case 1: {
                            s[0] = (h[0] + 2.0 * (h[1] + h[2] + h[3])) * c[0];
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

}
