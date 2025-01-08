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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

public class TurboRegTransformer {

    /**
     * Maximal number of registration iterations per level, when
     * speed is requested at the expense of accuracy. This number must be
     * corrected so that there are more iterations at the coarse levels
     * of the pyramid than at the fine levels.
     *
     * @see TurboRegTransformer#ITERATION_PROGRESSION
     **/
    private static final int FEW_ITERATIONS = 5;

    /**
     * Initial value of the Marquardt-Levenberg fudge factor.
     **/
    private static final double FIRST_LAMBDA = 1.0;

    /**
     * Update parameter of the Marquardt-Levenberg fudge factor.
     **/
    private static final double LAMBDA_MAGSTEP = 4.0;

    /**
     * Maximal number of registration iterations per level, when
     * accuracy is requested at the expense of speed. This number must be
     * corrected so that there are more iterations at the coarse levels
     * of the pyramid than at the fine levels.
     *
     * @see TurboRegTransformer#ITERATION_PROGRESSION
     **/
    private static final int MANY_ITERATIONS = 10;

    /**
     * Minimal update distance of the landmarks, in pixel units, when
     * accuracy is requested at the expense of speed. This distance does
     * not depend on the pyramid level.
     **/
    private static final double PIXEL_HIGH_PRECISION = 0.001;

    /**
     * Minimal update distance of the landmarks, in pixel units, when
     * speed is requested at the expense of accuracy. This distance does
     * not depend on the pyramid level.
     **/
    private static final double PIXEL_LOW_PRECISION = 0.1;

    /**
     * Multiplicative factor that determines how many more iterations
     * are allowed for a pyramid level one unit coarser.
     **/
    private static final int ITERATION_PROGRESSION = 2;

    private final double[] dxWeight = new double[4];
    private final double[] dyWeight = new double[4];
    private final double[] xWeight = new double[4];
    private final double[] yWeight = new double[4];
    private final int[] xIndex = new int[4];
    private final int[] yIndex = new int[4];
    private TurboRegImage sourceImg;
    private TurboRegImage targetImg;
    private TurboRegMask sourceMsk;
    private TurboRegMask targetMsk;
    private TurboRegPointHandler sourcePh;
    private TurboRegPointHandler targetPh;
    private double[][] sourcePoint;
    private double[][] targetPoint;
    private float[] inImg;
    private float[] outImg;
    private float[] xGradient;
    private float[] yGradient;
    private float[] inMsk;
    private float[] outMsk;
    private double targetJacobian;
    private double s;
    private double t;
    private double x;
    private double y;
    private double c0;
    private double c0u;
    private double c0v;
    private double c0uv;
    private double c1;
    private double c1u;
    private double c1v;
    private double c1uv;
    private double c2;
    private double c2u;
    private double c2v;
    private double c2uv;
    private double c3;
    private double c3u;
    private double c3v;
    private double c3uv;
    private double pixelPrecision;
    private int maxIterations;
    private int p;
    private int q;
    private int inNx;
    private int inNy;
    private int outNx;
    private int outNy;
    private int twiceInNx;
    private int twiceInNy;
    private TurboRegTransformationType transformation;
    private int pyramidDepth;
    private int iterationPower;
    private int iterationCost;
    private boolean accelerated;
    private boolean interactive;

    /**
     * Keep a local copy of most everything. Select among the pre-stored
     * constants.
     *
     * @param targetImg      Target image pyramid.
     * @param targetMsk      Target mask pyramid.
     * @param sourceImg      Source image pyramid.
     * @param sourceMsk      Source mask pyramid.
     * @param targetPh       Target <code>TurboRegPointHandler</code> object.
     * @param sourcePh       Source <code>TurboRegPointHandler</code> object.
     * @param transformation Transformation code.
     * @param accelerated    Trade-off between speed and accuracy.
     * @param interactive    Shows or hides the resulting image.
     **/
    public TurboRegTransformer(
            final TurboRegImage sourceImg,
            final TurboRegMask sourceMsk,
            final TurboRegPointHandler sourcePh,
            final TurboRegImage targetImg,
            final TurboRegMask targetMsk,
            final TurboRegPointHandler targetPh,
            final TurboRegTransformationType transformation,
            final boolean accelerated,
            final boolean interactive
    ) {
        this.sourceImg = sourceImg;
        this.sourceMsk = sourceMsk;
        this.sourcePh = sourcePh;
        this.targetImg = targetImg;
        this.targetMsk = targetMsk;
        this.targetPh = targetPh;
        this.transformation = transformation;
        this.accelerated = accelerated;
        this.interactive = interactive;
        sourcePoint = sourcePh.getPoints();
        targetPoint = targetPh.getPoints();
        if (accelerated) {
            pixelPrecision = PIXEL_LOW_PRECISION;
            maxIterations = FEW_ITERATIONS;
        } else {
            pixelPrecision = PIXEL_HIGH_PRECISION;
            maxIterations = MANY_ITERATIONS;
        }
    }

    /**
     * Append the current landmarks into a text file. Rigid format.
     *
     * @param pathAndFilename Path and name of the file where batch results
     *                        are being written.
     **/
    public void appendTransformation(
            final String pathAndFilename
    ) {
        outNx = targetImg.getWidth();
        outNy = targetImg.getHeight();
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        if (pathAndFilename == null) {
            return;
        }
        try {
            final FileWriter fw = new FileWriter(pathAndFilename, true);
            fw.write("\n");
            switch (transformation) {
                case Translation: {
                    fw.write("TRANSLATION\n");
                    break;
                }
                case RigidBody: {
                    fw.write("RIGID_BODY\n");
                    break;
                }
                case ScaledRotation: {
                    fw.write("SCALED_ROTATION\n");
                    break;
                }
                case Affine: {
                    fw.write("AFFINE\n");
                    break;
                }
                case Bilinear: {
                    fw.write("BILINEAR\n");
                    break;
                }
            }
            fw.write("\n");
            fw.write("Source size\n");
            fw.write(inNx + "\t" + inNy + "\n");
            fw.write("\n");
            fw.write("Target size\n");
            fw.write(outNx + "\t" + outNy + "\n");
            fw.write("\n");
            fw.write("Refined source landmarks\n");
            if (transformation == TurboRegTransformationType.RigidBody) {
                for (int i = 0; (i < transformation.getNativeValue()); i++) {
                    fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
                }
            } else {
                for (int i = 0; (i < (transformation.getNativeValue() / 2)); i++) {
                    fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
                }
            }
            fw.write("\n");
            fw.write("Target landmarks\n");
            if (transformation == TurboRegTransformationType.RigidBody) {
                for (int i = 0; (i < transformation.getNativeValue()); i++) {
                    fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
                }
            } else {
                for (int i = 0; (i < (transformation.getNativeValue() / 2)); i++) {
                    fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
                }
            }
            fw.close();
        } catch (IOException e) {
            IJ.log(
                    "IOException exception " + e.getMessage());
        } catch (SecurityException e) {
            IJ.log(
                    "Security exception " + e.getMessage());
        }
    }

    /**
     * Compute the final image.
     **/
    public void doBatchFinalTransform(
            final float[] pixels
    ) {
        if (accelerated) {
            inImg = sourceImg.getImage();
        } else {
            inImg = sourceImg.getCoefficient();
        }
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        outImg = pixels;
        outNx = targetImg.getWidth();
        outNy = targetImg.getHeight();
        final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        switch (transformation) {
            case Translation: {
                translationTransform(matrix);
                break;
            }
            case RigidBody:
            case ScaledRotation:
            case Affine: {
                affineTransform(matrix);
                break;
            }
            case Bilinear: {
                bilinearTransform(matrix);
                break;
            }
        }
    }

    /**
     * Compute the final image.
     **/
    public ImagePlus doFinalTransform(
            final int width,
            final int height
    ) {
        if (accelerated) {
            inImg = sourceImg.getImage();
        } else {
            inImg = sourceImg.getCoefficient();
        }
        inMsk = sourceMsk.getMask();
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        final ImageStack is = new ImageStack(width, height);
        final FloatProcessor dataFp = new FloatProcessor(width, height);
        is.addSlice("Data", dataFp);
        final FloatProcessor maskFp = new FloatProcessor(width, height);
        is.addSlice("Mask", maskFp);
        final ImagePlus imp = new ImagePlus("Output", is);
        imp.setSlice(1);
        outImg = (float[]) dataFp.getPixels();
        imp.setSlice(2);
        final float[] outMsk = (float[]) maskFp.getPixels();
        outNx = imp.getWidth();
        outNy = imp.getHeight();
        final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        switch (transformation) {
            case Translation: {
                translationTransform(matrix, outMsk);
                break;
            }
            case RigidBody:
            case ScaledRotation:
            case Affine: {
                affineTransform(matrix, outMsk);
                break;
            }
            case Bilinear: {
                bilinearTransform(matrix, outMsk);
                break;
            }
        }
        imp.setSlice(1);
        imp.getProcessor().resetMinAndMax();
        if (interactive) {
            imp.show();
            imp.updateAndDraw();
        }
        return (imp);
    }

    /**
     * Compute the final image.
     **/
    public float[] doFinalTransform(
            final TurboRegImage sourceImg,
            final TurboRegPointHandler sourcePh,
            final TurboRegImage targetImg,
            final TurboRegPointHandler targetPh,
            final TurboRegTransformationType transformation,
            final boolean accelerated
    ) {
        this.sourceImg = sourceImg;
        this.targetImg = targetImg;
        this.sourcePh = sourcePh;
        this.targetPh = targetPh;
        this.transformation = transformation;
        this.accelerated = accelerated;
        sourcePoint = sourcePh.getPoints();
        targetPoint = targetPh.getPoints();
        if (accelerated) {
            inImg = sourceImg.getImage();
        } else {
            inImg = sourceImg.getCoefficient();
        }
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        outNx = targetImg.getWidth();
        outNy = targetImg.getHeight();
        outImg = new float[outNx * outNy];
        final double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        switch (transformation) {
            case Translation: {
                translationTransform(matrix);
                break;
            }
            case RigidBody:
            case ScaledRotation:
            case Affine: {
                affineTransform(matrix);
                break;
            }
            case Bilinear: {
                bilinearTransform(matrix);
                break;
            }
        }
        return (outImg);
    }

    /**
     * Refine the landmarks.
     **/
    public void doRegistration(
    ) {
        Stack<?> sourceImgPyramid;
        Stack<?> sourceMskPyramid;
        Stack<?> targetImgPyramid;
        Stack<?> targetMskPyramid;
        if (sourceMsk == null) {
            sourceImgPyramid = sourceImg.getPyramid();
            sourceMskPyramid = null;
            targetImgPyramid = (Stack<?>) targetImg.getPyramid().clone();
            targetMskPyramid = (Stack<?>) targetMsk.getPyramid().clone();
        } else {
            sourceImgPyramid = sourceImg.getPyramid();
            sourceMskPyramid = sourceMsk.getPyramid();
            targetImgPyramid = targetImg.getPyramid();
            targetMskPyramid = targetMsk.getPyramid();
        }
        pyramidDepth = targetImg.getPyramidDepth();
        iterationPower = (int) Math.pow(
                (double) ITERATION_PROGRESSION, (double) pyramidDepth);
        iterationCost = 1;
        scaleBottomDownLandmarks();
        while (!targetImgPyramid.isEmpty()) {
            iterationPower /= ITERATION_PROGRESSION;
            if (transformation == TurboRegTransformationType.Bilinear) {
                inNx = (Integer) sourceImgPyramid.pop();
                inNy = (Integer) sourceImgPyramid.pop();
                inImg = (float[]) sourceImgPyramid.pop();
                if (sourceMskPyramid == null) {
                    inMsk = null;
                } else {
                    inMsk = (float[]) sourceMskPyramid.pop();
                }
                outNx = (Integer) targetImgPyramid.pop();
                outNy = (Integer) targetImgPyramid.pop();
                outImg = (float[]) targetImgPyramid.pop();
                outMsk = (float[]) targetMskPyramid.pop();
            } else {
                inNx = (Integer) targetImgPyramid.pop();
                inNy = (Integer) targetImgPyramid.pop();
                inImg = (float[]) targetImgPyramid.pop();
                inMsk = (float[]) targetMskPyramid.pop();
                outNx = (Integer) sourceImgPyramid.pop();
                outNy = (Integer) sourceImgPyramid.pop();
                outImg = (float[]) sourceImgPyramid.pop();
                xGradient = (float[]) sourceImgPyramid.pop();
                yGradient = (float[]) sourceImgPyramid.pop();
                if (sourceMskPyramid == null) {
                    outMsk = null;
                } else {
                    outMsk = (float[]) sourceMskPyramid.pop();
                }
            }
            twiceInNx = 2 * inNx;
            twiceInNy = 2 * inNy;
            switch (transformation) {
                case Translation: {
                    targetJacobian = 1.0;
                    inverseMarquardtLevenbergOptimization(
                            iterationPower * maxIterations - 1);
                    break;
                }
                case RigidBody: {
                    inverseMarquardtLevenbergRigidBodyOptimization(
                            iterationPower * maxIterations - 1);
                    break;
                }
                case ScaledRotation: {
                    targetJacobian = (targetPoint[0][0] - targetPoint[1][0])
                            * (targetPoint[0][0] - targetPoint[1][0])
                            + (targetPoint[0][1] - targetPoint[1][1])
                            * (targetPoint[0][1] - targetPoint[1][1]);
                    inverseMarquardtLevenbergOptimization(
                            iterationPower * maxIterations - 1);
                    break;
                }
                case Affine: {
                    targetJacobian = (targetPoint[1][0] - targetPoint[2][0])
                            * targetPoint[0][1]
                            + (targetPoint[2][0] - targetPoint[0][0])
                            * targetPoint[1][1]
                            + (targetPoint[0][0] - targetPoint[1][0])
                            * targetPoint[2][1];
                    inverseMarquardtLevenbergOptimization(
                            iterationPower * maxIterations - 1);
                    break;
                }
                case Bilinear: {
                    MarquardtLevenbergOptimization(
                            iterationPower * maxIterations - 1);
                    break;
                }
            }
            scaleUpLandmarks();
            sourcePh.setPoints(sourcePoint);
            iterationCost *= ITERATION_PROGRESSION;
        }
        iterationPower /= ITERATION_PROGRESSION;
        if (transformation == TurboRegTransformationType.Bilinear) {
            inNx = sourceImg.getWidth();
            inNy = sourceImg.getHeight();
            inImg = sourceImg.getCoefficient();
            if (sourceMsk == null) {
                inMsk = null;
            } else {
                inMsk = sourceMsk.getMask();
            }
            outNx = targetImg.getWidth();
            outNy = targetImg.getHeight();
            outImg = targetImg.getImage();
            outMsk = targetMsk.getMask();
        } else {
            inNx = targetImg.getWidth();
            inNy = targetImg.getHeight();
            inImg = targetImg.getCoefficient();
            inMsk = targetMsk.getMask();
            outNx = sourceImg.getWidth();
            outNy = sourceImg.getHeight();
            outImg = sourceImg.getImage();
            xGradient = sourceImg.getXGradient();
            yGradient = sourceImg.getYGradient();
            if (sourceMsk == null) {
                outMsk = null;
            } else {
                outMsk = sourceMsk.getMask();
            }
        }
        twiceInNx = 2 * inNx;
        twiceInNy = 2 * inNy;
        if (accelerated) {
//
//                    iterationCost * (maxIterations - 1));
        } else {
            switch (transformation) {
                case RigidBody: {
                    inverseMarquardtLevenbergRigidBodyOptimization(
                            maxIterations - 1);
                    break;
                }
                case Translation:
                case ScaledRotation:
                case Affine: {
                    inverseMarquardtLevenbergOptimization(maxIterations - 1);
                    break;
                }
                case Bilinear: {
                    MarquardtLevenbergOptimization(maxIterations - 1);
                    break;
                }
            }
        }
        sourcePh.setPoints(sourcePoint);
        iterationPower = (int) Math.pow(
                (double) ITERATION_PROGRESSION, (double) pyramidDepth);
    }

    /**
     * Save the current landmarks into a text file and return the path
     * and name of the file. Rigid format.
     **/
    public String saveTransformation(
            String filename
    ) {
        inNx = sourceImg.getWidth();
        inNy = sourceImg.getHeight();
        outNx = targetImg.getWidth();
        outNy = targetImg.getHeight();
        String path = "";
        if (filename == null) {
            final Frame f = new Frame();
            final FileDialog fd = new FileDialog(
                    f, "Save landmarks", FileDialog.SAVE);
            filename = "landmarks.txt";
            fd.setFile(filename);
            fd.setVisible(true);
            path = fd.getDirectory();
            filename = fd.getFile();
            if ((path == null) || (filename == null)) {
                return ("");
            }
        }
        try {
            final FileWriter fw = new FileWriter(path + filename);
            fw.write("Transformation\n");
            switch (transformation) {
                case Translation: {
                    fw.write("TRANSLATION\n");
                    break;
                }
                case RigidBody: {
                    fw.write("RIGID_BODY\n");
                    break;
                }
                case ScaledRotation: {
                    fw.write("SCALED_ROTATION\n");
                    break;
                }
                case Affine: {
                    fw.write("AFFINE\n");
                    break;
                }
                case Bilinear: {
                    fw.write("BILINEAR\n");
                    break;
                }
            }
            fw.write("\n");
            fw.write("Source size\n");
            fw.write(inNx + "\t" + inNy + "\n");
            fw.write("\n");
            fw.write("Target size\n");
            fw.write(outNx + "\t" + outNy + "\n");
            fw.write("\n");
            fw.write("Refined source landmarks\n");
            if (transformation == TurboRegTransformationType.RigidBody) {
                for (int i = 0; (i < transformation.getNativeValue()); i++) {
                    fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
                }
            } else {
                for (int i = 0; (i < (transformation.getNativeValue() / 2)); i++) {
                    fw.write(sourcePoint[i][0] + "\t" + sourcePoint[i][1] + "\n");
                }
            }
            fw.write("\n");
            fw.write("Target landmarks\n");
            if (transformation == TurboRegTransformationType.RigidBody) {
                for (int i = 0; (i < transformation.getNativeValue()); i++) {
                    fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
                }
            } else {
                for (int i = 0; (i < (transformation.getNativeValue() / 2)); i++) {
                    fw.write(targetPoint[i][0] + "\t" + targetPoint[i][1] + "\n");
                }
            }
            fw.close();
        } catch (IOException e) {
            IJ.log(
                    "IOException exception " + e.getMessage());
        } catch (SecurityException e) {
            IJ.log(
                    "Security exception " + e.getMessage());
        }
        return (path + filename);
    }

    private void affineTransform(
            final double[][] matrix
    ) {
        double yx;
        double yy;
        double x0;
        double y0;
        int xMsk;
        int yMsk;
        int k = 0;
        yx = matrix[0][0];
        yy = matrix[1][0];
        for (int v = 0; (v < outNy); v++) {
            x0 = yx;
            y0 = yy;
            for (int u = 0; (u < outNx); u++) {
                x = x0;
                y = y0;
                xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                    xMsk += yMsk * inNx;
                    if (accelerated) {
                        outImg[k++] = inImg[xMsk];
                    } else {
                        xIndexes();
                        yIndexes();
                        x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                        y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                        xWeights();
                        yWeights();
                        outImg[k++] = (float) interpolate();
                    }
                } else {
                    outImg[k++] = 0.0F;
                }
                x0 += matrix[0][1];
                y0 += matrix[1][1];
            }
            yx += matrix[0][2];
            yy += matrix[1][2];
        }
    }

    private void affineTransform(
            final double[][] matrix,
            final float[] outMsk
    ) {
        double yx;
        double yy;
        double x0;
        double y0;
        int xMsk;
        int yMsk;
        int k = 0;
        yx = matrix[0][0];
        yy = matrix[1][0];
        for (int v = 0; (v < outNy); v++) {
            x0 = yx;
            y0 = yy;
            for (int u = 0; (u < outNx); u++) {
                x = x0;
                y = y0;
                xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                    xMsk += yMsk * inNx;
                    if (accelerated) {
                        outImg[k] = inImg[xMsk];
                    } else {
                        xIndexes();
                        yIndexes();
                        x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                        y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                        xWeights();
                        yWeights();
                        outImg[k] = (float) interpolate();
                    }
                    outMsk[k++] = inMsk[xMsk];
                } else {
                    outImg[k] = 0.0F;
                    outMsk[k++] = 0.0F;
                }
                x0 += matrix[0][1];
                y0 += matrix[1][1];
            }
            yx += matrix[0][2];
            yy += matrix[1][2];

        }

    } /* affineTransform */

    /*------------------------------------------------------------------*/
    private void bilinearTransform(
            final double[][] matrix
    ) {
        double yx;
        double yy;
        double yxy;
        double yyy;
        double x0;
        double y0;
        int xMsk;
        int yMsk;
        int k = 0;

        yx = matrix[0][0];
        yy = matrix[1][0];
        yxy = 0.0;
        yyy = 0.0;
        for (int v = 0; (v < outNy); v++) {
            x0 = yx;
            y0 = yy;
            for (int u = 0; (u < outNx); u++) {
                x = x0;
                y = y0;
                xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                    xMsk += yMsk * inNx;
                    if (accelerated) {
                        outImg[k++] = inImg[xMsk];
                    } else {
                        xIndexes();
                        yIndexes();
                        x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                        y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                        xWeights();
                        yWeights();
                        outImg[k++] = (float) interpolate();
                    }
                } else {
                    outImg[k++] = 0.0F;
                }
                x0 += matrix[0][1] + yxy;
                y0 += matrix[1][1] + yyy;
            }
            yx += matrix[0][2];
            yy += matrix[1][2];
            yxy += matrix[0][3];
            yyy += matrix[1][3];

        }

    } /* bilinearTransform */

    /*------------------------------------------------------------------*/
    private void bilinearTransform(
            final double[][] matrix,
            final float[] outMsk
    ) {
        double yx;
        double yy;
        double yxy;
        double yyy;
        double x0;
        double y0;
        int xMsk;
        int yMsk;
        int k = 0;

        yx = matrix[0][0];
        yy = matrix[1][0];
        yxy = 0.0;
        yyy = 0.0;
        for (int v = 0; (v < outNy); v++) {
            x0 = yx;
            y0 = yy;
            for (int u = 0; (u < outNx); u++) {
                x = x0;
                y = y0;
                xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= xMsk) && (xMsk < inNx) && (0 <= yMsk) && (yMsk < inNy)) {
                    xMsk += yMsk * inNx;
                    if (accelerated) {
                        outImg[k] = inImg[xMsk];
                    } else {
                        xIndexes();
                        yIndexes();
                        x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                        y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                        xWeights();
                        yWeights();
                        outImg[k] = (float) interpolate();
                    }
                    outMsk[k++] = inMsk[xMsk];
                } else {
                    outImg[k] = 0.0F;
                    outMsk[k++] = 0.0F;
                }
                x0 += matrix[0][1] + yxy;
                y0 += matrix[1][1] + yyy;
            }
            yx += matrix[0][2];
            yy += matrix[1][2];
            yxy += matrix[0][3];
            yyy += matrix[1][3];

        }

    } /* bilinearTransform */

    /*------------------------------------------------------------------*/
    private void computeBilinearGradientConstants(
    ) {
        final double u1 = targetPoint[0][0];
        final double u2 = targetPoint[1][0];
        final double u3 = targetPoint[2][0];
        final double u4 = targetPoint[3][0];
        final double v1 = targetPoint[0][1];
        final double v2 = targetPoint[1][1];
        final double v3 = targetPoint[2][1];
        final double v4 = targetPoint[3][1];
        final double u12 = u1 - u2;
        final double u13 = u1 - u3;
        final double u14 = u1 - u4;
        final double u23 = u2 - u3;
        final double u24 = u2 - u4;
        final double u34 = u3 - u4;
        final double v12 = v1 - v2;
        final double v13 = v1 - v3;
        final double v14 = v1 - v4;
        final double v23 = v2 - v3;
        final double v24 = v2 - v4;
        final double v34 = v3 - v4;
        final double uv12 = u1 * u2 * v12;
        final double uv13 = u1 * u3 * v13;
        final double uv14 = u1 * u4 * v14;
        final double uv23 = u2 * u3 * v23;
        final double uv24 = u2 * u4 * v24;
        final double uv34 = u3 * u4 * v34;
        final double det = uv12 * v34 - uv13 * v24 + uv14 * v23 + uv23 * v14
                - uv24 * v13 + uv34 * v12;
        c0 = (-uv34 * v2 + uv24 * v3 - uv23 * v4) / det;
        c0u = (u3 * v3 * v24 - u2 * v2 * v34 - u4 * v4 * v23) / det;
        c0v = (uv23 - uv24 + uv34) / det;
        c0uv = (u4 * v23 - u3 * v24 + u2 * v34) / det;
        c1 = (uv34 * v1 - uv14 * v3 + uv13 * v4) / det;
        c1u = (-u3 * v3 * v14 + u1 * v1 * v34 + u4 * v4 * v13) / det;
        c1v = (-uv13 + uv14 - uv34) / det;
        c1uv = (-u4 * v13 + u3 * v14 - u1 * v34) / det;
        c2 = (-uv24 * v1 + uv14 * v2 - uv12 * v4) / det;
        c2u = (u2 * v2 * v14 - u1 * v1 * v24 - u4 * v4 * v12) / det;
        c2v = (uv12 - uv14 + uv24) / det;
        c2uv = (u4 * v12 - u2 * v14 + u1 * v24) / det;
        c3 = (uv23 * v1 - uv13 * v2 + uv12 * v3) / det;
        c3u = (-u2 * v2 * v13 + u1 * v1 * v23 + u3 * v3 * v12) / det;
        c3v = (-uv12 + uv13 - uv23) / det;
        c3uv = (-u3 * v1 + u2 * v13 + u3 * v2 - u1 * v23) / det;
    }

    /*------------------------------------------------------------------*/
    private double getAffineMeanSquares(
            final double[][] sourcePoint,
            final double[][] matrix
    ) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double u3 = sourcePoint[2][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double v3 = sourcePoint[2][1];
        final double uv32 = u3 * v2 - u2 * v3;
        final double uv21 = u2 * v1 - u1 * v2;
        final double uv13 = u1 * v3 - u3 * v1;
        final double det = uv32 + uv21 + uv13;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / ((double) area * Math.abs(det / targetJacobian)));
    } /* getAffineMeanSquares */

    /*------------------------------------------------------------------*/
    private double getAffineMeanSquares(
            final double[][] sourcePoint,
            final double[][] matrix,
            final double[] gradient
    ) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double u3 = sourcePoint[2][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double v3 = sourcePoint[2][1];
        double uv32 = u3 * v2 - u2 * v3;
        double uv21 = u2 * v1 - u1 * v2;
        double uv13 = u1 * v3 - u3 * v1;
        final double det = uv32 + uv21 + uv13;
        final double u12 = (u1 - u2) / det;
        final double u23 = (u2 - u3) / det;
        final double u31 = (u3 - u1) / det;
        final double v12 = (v1 - v2) / det;
        final double v23 = (v2 - v3) / det;
        final double v31 = (v3 - v1) / det;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double g0;
        double g1;
        double g2;
        double dx0;
        double dx1;
        double dx2;
        double dy0;
        double dy1;
        double dy2;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        uv32 /= det;
        uv21 /= det;
        uv13 /= det;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = u23 * (double) v - v23 * (double) u + uv32;
                            g1 = u31 * (double) v - v31 * (double) u + uv13;
                            g2 = u12 * (double) v - v12 * (double) u + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = u23 * (double) v - v23 * (double) u + uv32;
                            g1 = u31 * (double) v - v31 * (double) u + uv13;
                            g2 = u12 * (double) v - v12 * (double) u + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / ((double) area * Math.abs(det / targetJacobian)));
    } /* getAffineMeanSquares */

    /*------------------------------------------------------------------*/
    private double getAffineMeanSquares(
            final double[][] sourcePoint,
            final double[][] matrix,
            final double[][] hessian,
            final double[] gradient
    ) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double u3 = sourcePoint[2][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double v3 = sourcePoint[2][1];
        double uv32 = u3 * v2 - u2 * v3;
        double uv21 = u2 * v1 - u1 * v2;
        double uv13 = u1 * v3 - u3 * v1;
        final double det = uv32 + uv21 + uv13;
        final double u12 = (u1 - u2) / det;
        final double u23 = (u2 - u3) / det;
        final double u31 = (u3 - u1) / det;
        final double v12 = (v1 - v2) / det;
        final double v23 = (v2 - v3) / det;
        final double v31 = (v3 - v1) / det;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double g0;
        double g1;
        double g2;
        double dx0;
        double dx1;
        double dx2;
        double dy0;
        double dy1;
        double dy2;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        uv32 /= det;
        uv21 /= det;
        uv13 /= det;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNativeValue()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = u23 * (double) v - v23 * (double) u + uv32;
                            g1 = u31 * (double) v - v31 * (double) u + uv13;
                            g2 = u12 * (double) v - v12 * (double) u + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[0][4] += dx0 * dx2;
                            hessian[0][5] += dx0 * dy2;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[1][4] += dy0 * dx2;
                            hessian[1][5] += dy0 * dy2;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[2][4] += dx1 * dx2;
                            hessian[2][5] += dx1 * dy2;
                            hessian[3][3] += dy1 * dy1;
                            hessian[3][4] += dy1 * dx2;
                            hessian[3][5] += dy1 * dy2;
                            hessian[4][4] += dx2 * dx2;
                            hessian[4][5] += dx2 * dy2;
                            hessian[5][5] += dy2 * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            g0 = u23 * (double) v - v23 * (double) u + uv32;
                            g1 = u31 * (double) v - v31 * (double) u + uv13;
                            g2 = u12 * (double) v - v12 * (double) u + uv21;
                            dx0 = xGradient[k] * g0;
                            dy0 = yGradient[k] * g0;
                            dx1 = xGradient[k] * g1;
                            dy1 = yGradient[k] * g1;
                            dx2 = xGradient[k] * g2;
                            dy2 = yGradient[k] * g2;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[0][4] += dx0 * dx2;
                            hessian[0][5] += dx0 * dy2;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[1][4] += dy0 * dx2;
                            hessian[1][5] += dy0 * dy2;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[2][4] += dx1 * dx2;
                            hessian[2][5] += dx1 * dy2;
                            hessian[3][3] += dy1 * dy1;
                            hessian[3][4] += dy1 * dx2;
                            hessian[3][5] += dy1 * dy2;
                            hessian[4][4] += dx2 * dx2;
                            hessian[4][5] += dx2 * dy2;
                            hessian[5][5] += dy2 * dy2;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        for (int i = 1; (i < transformation.getNativeValue()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / ((double) area * Math.abs(det / targetJacobian)));
    } /* getAffineMeanSquares */

    /*------------------------------------------------------------------*/
    private double getBilinearMeanSquares(
            final double[][] matrix
    ) {
        double yx;
        double yy;
        double yxy;
        double yyy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        if (inMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            yxy = 0.0;
            yyy = 0.0;
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    if (outMsk[k] != 0.0F) {
                        x = x0;
                        y = y0;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)
                                && (0 <= yMsk) && (yMsk < inNy)) {
                            xIndexes();
                            yIndexes();
                            area++;
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = interpolate() - (double) outImg[k];
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1] + yxy;
                    y0 += matrix[1][1] + yyy;
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
                yxy += matrix[0][3];
                yyy += matrix[1][3];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            yxy = 0.0;
            yyy = 0.0;
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        xMsk += yMsk * inNx;
                        if ((outMsk[k] * inMsk[xMsk]) != 0.0F) {
                            xIndexes();
                            yIndexes();
                            area++;
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = interpolate() - (double) outImg[k];
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1] + yxy;
                    y0 += matrix[1][1] + yyy;
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
                yxy += matrix[0][3];
                yyy += matrix[1][3];
            }
        }
        return (meanSquares / (double) area);
    } /* getBilinearMeanSquares */

    /*------------------------------------------------------------------*/
    private double getBilinearMeanSquares(
            final double[][] matrix,
            final double[][] hessian,
            final double[] gradient
    ) {
        double yx;
        double yy;
        double yxy;
        double yyy;
        double x0;
        double y0;
        double uv;
        double xGradient;
        double yGradient;
        double difference;
        double meanSquares = 0.0;
        double g0;
        double g1;
        double g2;
        double g3;
        double dx0;
        double dx1;
        double dx2;
        double dx3;
        double dy0;
        double dy1;
        double dy2;
        double dy3;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        computeBilinearGradientConstants();
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNativeValue()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        if (inMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            yxy = 0.0;
            yyy = 0.0;
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    if (outMsk[k] != 0.0F) {
                        x = x0;
                        y = y0;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)
                                && (0 <= yMsk) && (yMsk < inNy)) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xDxWeights();
                            yDyWeights();
                            difference = interpolate() - (double) outImg[k];
                            meanSquares += difference * difference;
                            xGradient = interpolateDx();
                            yGradient = interpolateDy();
                            uv = (double) u * (double) v;
                            g0 = c0uv * uv + c0u * (double) u + c0v * (double) v + c0;
                            g1 = c1uv * uv + c1u * (double) u + c1v * (double) v + c1;
                            g2 = c2uv * uv + c2u * (double) u + c2v * (double) v + c2;
                            g3 = c3uv * uv + c3u * (double) u + c3v * (double) v + c3;
                            dx0 = xGradient * g0;
                            dy0 = yGradient * g0;
                            dx1 = xGradient * g1;
                            dy1 = yGradient * g1;
                            dx2 = xGradient * g2;
                            dy2 = yGradient * g2;
                            dx3 = xGradient * g3;
                            dy3 = yGradient * g3;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                            gradient[6] += difference * dx3;
                            gradient[7] += difference * dy3;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[0][4] += dx0 * dx2;
                            hessian[0][5] += dx0 * dy2;
                            hessian[0][6] += dx0 * dx3;
                            hessian[0][7] += dx0 * dy3;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[1][4] += dy0 * dx2;
                            hessian[1][5] += dy0 * dy2;
                            hessian[1][6] += dy0 * dx3;
                            hessian[1][7] += dy0 * dy3;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[2][4] += dx1 * dx2;
                            hessian[2][5] += dx1 * dy2;
                            hessian[2][6] += dx1 * dx3;
                            hessian[2][7] += dx1 * dy3;
                            hessian[3][3] += dy1 * dy1;
                            hessian[3][4] += dy1 * dx2;
                            hessian[3][5] += dy1 * dy2;
                            hessian[3][6] += dy1 * dx3;
                            hessian[3][7] += dy1 * dy3;
                            hessian[4][4] += dx2 * dx2;
                            hessian[4][5] += dx2 * dy2;
                            hessian[4][6] += dx2 * dx3;
                            hessian[4][7] += dx2 * dy3;
                            hessian[5][5] += dy2 * dy2;
                            hessian[5][6] += dy2 * dx3;
                            hessian[5][7] += dy2 * dy3;
                            hessian[6][6] += dx3 * dx3;
                            hessian[6][7] += dx3 * dy3;
                            hessian[7][7] += dy3 * dy3;
                        }
                    }
                    x0 += matrix[0][1] + yxy;
                    y0 += matrix[1][1] + yyy;
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
                yxy += matrix[0][3];
                yyy += matrix[1][3];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            yxy = 0.0;
            yyy = 0.0;
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        xMsk += yMsk * inNx;
                        if ((outMsk[k] * inMsk[xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xDxWeights();
                            yDyWeights();
                            difference = interpolate() - (double) outImg[k];
                            meanSquares += difference * difference;
                            xGradient = interpolateDx();
                            yGradient = interpolateDy();
                            uv = (double) u * (double) v;
                            g0 = c0uv * uv + c0u * (double) u + c0v * (double) v + c0;
                            g1 = c1uv * uv + c1u * (double) u + c1v * (double) v + c1;
                            g2 = c2uv * uv + c2u * (double) u + c2v * (double) v + c2;
                            g3 = c3uv * uv + c3u * (double) u + c3v * (double) v + c3;
                            dx0 = xGradient * g0;
                            dy0 = yGradient * g0;
                            dx1 = xGradient * g1;
                            dy1 = yGradient * g1;
                            dx2 = xGradient * g2;
                            dy2 = yGradient * g2;
                            dx3 = xGradient * g3;
                            dy3 = yGradient * g3;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            gradient[4] += difference * dx2;
                            gradient[5] += difference * dy2;
                            gradient[6] += difference * dx3;
                            gradient[7] += difference * dy3;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[0][4] += dx0 * dx2;
                            hessian[0][5] += dx0 * dy2;
                            hessian[0][6] += dx0 * dx3;
                            hessian[0][7] += dx0 * dy3;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[1][4] += dy0 * dx2;
                            hessian[1][5] += dy0 * dy2;
                            hessian[1][6] += dy0 * dx3;
                            hessian[1][7] += dy0 * dy3;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[2][4] += dx1 * dx2;
                            hessian[2][5] += dx1 * dy2;
                            hessian[2][6] += dx1 * dx3;
                            hessian[2][7] += dx1 * dy3;
                            hessian[3][3] += dy1 * dy1;
                            hessian[3][4] += dy1 * dx2;
                            hessian[3][5] += dy1 * dy2;
                            hessian[3][6] += dy1 * dx3;
                            hessian[3][7] += dy1 * dy3;
                            hessian[4][4] += dx2 * dx2;
                            hessian[4][5] += dx2 * dy2;
                            hessian[4][6] += dx2 * dx3;
                            hessian[4][7] += dx2 * dy3;
                            hessian[5][5] += dy2 * dy2;
                            hessian[5][6] += dy2 * dx3;
                            hessian[5][7] += dy2 * dy3;
                            hessian[6][6] += dx3 * dx3;
                            hessian[6][7] += dx3 * dy3;
                            hessian[7][7] += dy3 * dy3;
                        }
                    }
                    x0 += matrix[0][1] + yxy;
                    y0 += matrix[1][1] + yyy;
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
                yxy += matrix[0][3];
                yyy += matrix[1][3];
            }
        }
        for (int i = 1; (i < transformation.getNativeValue()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / (double) area);
    } /* getBilinearMeanSquares */

    /*------------------------------------------------------------------*/
    private double getRigidBodyMeanSquares(
            final double[][] matrix
    ) {
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / (double) area);
    } /* getRigidBodyMeanSquares */

    /*------------------------------------------------------------------*/
    private double getRigidBodyMeanSquares(
            final double[][] matrix,
            final double[] gradient

    ) {
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gradient[0] += difference * (yGradient[k] * (double) u
                                    - xGradient[k] * (double) v);
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gradient[0] += difference * (yGradient[k] * (double) u
                                    - xGradient[k] * (double) v);
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / (double) area);
    } /* getRigidBodyMeanSquares */

    /*------------------------------------------------------------------*/
    private double getRigidBodyMeanSquares(
            final double[][] matrix,
            final double[][] hessian,
            final double[] gradient
    ) {
        double yx;
        double yy;
        double x0;
        double y0;
        double dTheta;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNativeValue()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            dTheta = yGradient[k] * (double) u
                                    - xGradient[k] * (double) v;
                            gradient[0] += difference * dTheta;
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                            hessian[0][0] += dTheta * dTheta;
                            hessian[0][1] += dTheta * xGradient[k];
                            hessian[0][2] += dTheta * yGradient[k];
                            hessian[1][1] += xGradient[k] * xGradient[k];
                            hessian[1][2] += xGradient[k] * yGradient[k];
                            hessian[2][2] += yGradient[k] * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            dTheta = yGradient[k] * (double) u
                                    - xGradient[k] * (double) v;
                            gradient[0] += difference * dTheta;
                            gradient[1] += difference * xGradient[k];
                            gradient[2] += difference * yGradient[k];
                            hessian[0][0] += dTheta * dTheta;
                            hessian[0][1] += dTheta * xGradient[k];
                            hessian[0][2] += dTheta * yGradient[k];
                            hessian[1][1] += xGradient[k] * xGradient[k];
                            hessian[1][2] += xGradient[k] * yGradient[k];
                            hessian[2][2] += yGradient[k] * yGradient[k];
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        for (int i = 1; (i < transformation.getNativeValue()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / (double) area);
    } /* getRigidBodyMeanSquares */

    /*------------------------------------------------------------------*/
    private double getScaledRotationMeanSquares(
            final double[][] sourcePoint,
            final double[][] matrix
    ) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double u12 = u1 - u2;
        final double v12 = v1 - v2;
        final double uv2 = u12 * u12 + v12 * v12;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / ((double) area * uv2 / targetJacobian));
    } /* getScaledRotationMeanSquares */

    /*------------------------------------------------------------------*/
    private double getScaledRotationMeanSquares(
            final double[][] sourcePoint,
            final double[][] matrix,
            final double[] gradient
    ) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double u12 = u1 - u2;
        final double v12 = v1 - v2;
        final double uv2 = u12 * u12 + v12 * v12;
        final double c = 0.5 * (u2 * v1 - u1 * v2) / uv2;
        final double c1 = u12 / uv2;
        final double c2 = v12 / uv2;
        final double c3 = (uv2 - u12 * v12) / uv2;
        final double c4 = (uv2 + u12 * v12) / uv2;
        final double c5 = c + u1 * c1 + u2 * c2;
        final double c6 = c * (u12 * u12 - v12 * v12) / uv2;
        final double c7 = c1 * c4;
        final double c8 = c1 - c2 - c1 * c2 * v12;
        final double c9 = c1 + c2 - c1 * c2 * u12;
        final double c0 = c2 * c3;
        final double dgxx0 = c1 * u2 + c2 * v2;
        final double dgyx0 = 2.0 * c;
        final double dgxx1 = c5 + c6;
        final double dgyy1 = c5 - c6;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double gxx0;
        double gxx1;
        double gxy0;
        double gxy1;
        double gyx0;
        double gyx1;
        double gyy0;
        double gyy1;
        double dx0;
        double dx1;
        double dy0;
        double dy1;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = (double) u * c1 + (double) v * c2 - dgxx0;
                            gyx0 = (double) v * c1 - (double) u * c2 + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = (double) v * c8 - (double) u * c7 + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (double) u * c9 - (double) v * c0;
                            dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
                            dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
                            dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
                            dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = (double) u * c1 + (double) v * c2 - dgxx0;
                            gyx0 = (double) v * c1 - (double) u * c2 + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = (double) v * c8 - (double) u * c7 + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (double) u * c9 - (double) v * c0;
                            dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
                            dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
                            dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
                            dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        return (meanSquares / ((double) area * uv2 / targetJacobian));
    } /* getScaledRotationMeanSquares */

    /*------------------------------------------------------------------*/
    private double getScaledRotationMeanSquares(
            final double[][] sourcePoint,
            final double[][] matrix,
            final double[][] hessian,
            final double[] gradient
    ) {
        final double u1 = sourcePoint[0][0];
        final double u2 = sourcePoint[1][0];
        final double v1 = sourcePoint[0][1];
        final double v2 = sourcePoint[1][1];
        final double u12 = u1 - u2;
        final double v12 = v1 - v2;
        final double uv2 = u12 * u12 + v12 * v12;
        final double c = 0.5 * (u2 * v1 - u1 * v2) / uv2;
        final double c1 = u12 / uv2;
        final double c2 = v12 / uv2;
        final double c3 = (uv2 - u12 * v12) / uv2;
        final double c4 = (uv2 + u12 * v12) / uv2;
        final double c5 = c + u1 * c1 + u2 * c2;
        final double c6 = c * (u12 * u12 - v12 * v12) / uv2;
        final double c7 = c1 * c4;
        final double c8 = c1 - c2 - c1 * c2 * v12;
        final double c9 = c1 + c2 - c1 * c2 * u12;
        final double c0 = c2 * c3;
        final double dgxx0 = c1 * u2 + c2 * v2;
        final double dgyx0 = 2.0 * c;
        final double dgxx1 = c5 + c6;
        final double dgyy1 = c5 - c6;
        double yx;
        double yy;
        double x0;
        double y0;
        double difference;
        double meanSquares = 0.0;
        double gxx0;
        double gxx1;
        double gxy0;
        double gxy1;
        double gyx0;
        double gyx1;
        double gyy0;
        double gyy1;
        double dx0;
        double dx1;
        double dy0;
        double dy1;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNativeValue()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        if (outMsk == null) {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if (inMsk[yMsk * inNx + xMsk] != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = (double) u * c1 + (double) v * c2 - dgxx0;
                            gyx0 = (double) v * c1 - (double) u * c2 + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = (double) v * c8 - (double) u * c7 + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (double) u * c9 - (double) v * c0;
                            dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
                            dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
                            dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
                            dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[3][3] += dy1 * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        } else {
            yx = matrix[0][0];
            yy = matrix[1][0];
            for (int v = 0; (v < outNy); v++) {
                x0 = yx;
                y0 = yy;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = x0;
                    y = y0;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)
                            && (0 <= yMsk) && (yMsk < inNy)) {
                        if ((outMsk[k] * inMsk[yMsk * inNx + xMsk]) != 0.0F) {
                            area++;
                            xIndexes();
                            yIndexes();
                            x -= (0.0 <= x) ? ((int) x) : ((int) x - 1);
                            y -= (0.0 <= y) ? ((int) y) : ((int) y - 1);
                            xWeights();
                            yWeights();
                            difference = (double) outImg[k] - interpolate();
                            meanSquares += difference * difference;
                            gxx0 = (double) u * c1 + (double) v * c2 - dgxx0;
                            gyx0 = (double) v * c1 - (double) u * c2 + dgyx0;
                            gxy0 = -gyx0;
                            gyy0 = gxx0;
                            gxx1 = (double) v * c8 - (double) u * c7 + dgxx1;
                            gyx1 = -c3 * gyx0;
                            gxy1 = c4 * gyx0;
                            gyy1 = dgyy1 - (double) u * c9 - (double) v * c0;
                            dx0 = xGradient[k] * gxx0 + yGradient[k] * gyx0;
                            dy0 = xGradient[k] * gxy0 + yGradient[k] * gyy0;
                            dx1 = xGradient[k] * gxx1 + yGradient[k] * gyx1;
                            dy1 = xGradient[k] * gxy1 + yGradient[k] * gyy1;
                            gradient[0] += difference * dx0;
                            gradient[1] += difference * dy0;
                            gradient[2] += difference * dx1;
                            gradient[3] += difference * dy1;
                            hessian[0][0] += dx0 * dx0;
                            hessian[0][1] += dx0 * dy0;
                            hessian[0][2] += dx0 * dx1;
                            hessian[0][3] += dx0 * dy1;
                            hessian[1][1] += dy0 * dy0;
                            hessian[1][2] += dy0 * dx1;
                            hessian[1][3] += dy0 * dy1;
                            hessian[2][2] += dx1 * dx1;
                            hessian[2][3] += dx1 * dy1;
                            hessian[3][3] += dy1 * dy1;
                        }
                    }
                    x0 += matrix[0][1];
                    y0 += matrix[1][1];
                }
                yx += matrix[0][2];
                yy += matrix[1][2];
            }
        }
        for (int i = 1; (i < transformation.getNativeValue()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / ((double) area * uv2 / targetJacobian));
    } /* getScaledRotationMeanSquares */

    /*------------------------------------------------------------------*/
    private double[][] getTransformationMatrix(
            final double[][] fromCoord,
            final double[][] toCoord
    ) {
        double[][] matrix = null;
        double[][] a = null;
        double[] v = null;
        switch (transformation) {
            case Translation: {
                matrix = new double[2][1];
                matrix[0][0] = toCoord[0][0] - fromCoord[0][0];
                matrix[1][0] = toCoord[0][1] - fromCoord[0][1];
                break;
            }
            case RigidBody: {
                final double angle = Math.atan2(fromCoord[2][0] - fromCoord[1][0],
                        fromCoord[2][1] - fromCoord[1][1])
                        - Math.atan2(toCoord[2][0] - toCoord[1][0],
                        toCoord[2][1] - toCoord[1][1]);
                final double c = Math.cos(angle);
                final double s = Math.sin(angle);
                matrix = new double[2][3];
                matrix[0][0] = toCoord[0][0]
                        - c * fromCoord[0][0] + s * fromCoord[0][1];
                matrix[0][1] = c;
                matrix[0][2] = -s;
                matrix[1][0] = toCoord[0][1]
                        - s * fromCoord[0][0] - c * fromCoord[0][1];
                matrix[1][1] = s;
                matrix[1][2] = c;
                break;
            }
            case ScaledRotation: {
                matrix = new double[2][3];
                a = new double[3][3];
                v = new double[3];
                a[0][0] = 1.0;
                a[0][1] = fromCoord[0][0];
                a[0][2] = fromCoord[0][1];
                a[1][0] = 1.0;
                a[1][1] = fromCoord[1][0];
                a[1][2] = fromCoord[1][1];
                a[2][0] = 1.0;
                a[2][1] = fromCoord[0][1] - fromCoord[1][1] + fromCoord[1][0];
                a[2][2] = fromCoord[1][0] + fromCoord[1][1] - fromCoord[0][0];
                invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[0][1] - toCoord[1][1] + toCoord[1][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[0][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }
                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[1][0] + toCoord[1][1] - toCoord[0][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[1][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[1][i] += a[i][j] * v[j];
                    }
                }
                break;
            }
            case Affine: {
                matrix = new double[2][3];
                a = new double[3][3];
                v = new double[3];
                a[0][0] = 1.0;
                a[0][1] = fromCoord[0][0];
                a[0][2] = fromCoord[0][1];
                a[1][0] = 1.0;
                a[1][1] = fromCoord[1][0];
                a[1][2] = fromCoord[1][1];
                a[2][0] = 1.0;
                a[2][1] = fromCoord[2][0];
                a[2][2] = fromCoord[2][1];
                invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[2][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[0][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }
                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[2][1];
                for (int i = 0; (i < 3); i++) {
                    matrix[1][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[1][i] += a[i][j] * v[j];
                    }
                }
                break;
            }
            case Bilinear: {
                matrix = new double[2][4];
                a = new double[4][4];
                v = new double[4];
                a[0][0] = 1.0;
                a[0][1] = fromCoord[0][0];
                a[0][2] = fromCoord[0][1];
                a[0][3] = fromCoord[0][0] * fromCoord[0][1];
                a[1][0] = 1.0;
                a[1][1] = fromCoord[1][0];
                a[1][2] = fromCoord[1][1];
                a[1][3] = fromCoord[1][0] * fromCoord[1][1];
                a[2][0] = 1.0;
                a[2][1] = fromCoord[2][0];
                a[2][2] = fromCoord[2][1];
                a[2][3] = fromCoord[2][0] * fromCoord[2][1];
                a[3][0] = 1.0;
                a[3][1] = fromCoord[3][0];
                a[3][2] = fromCoord[3][1];
                a[3][3] = fromCoord[3][0] * fromCoord[3][1];
                invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[2][0];
                v[3] = toCoord[3][0];
                for (int i = 0; (i < 4); i++) {
                    matrix[0][i] = 0.0;
                    for (int j = 0; (j < 4); j++) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }
                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[2][1];
                v[3] = toCoord[3][1];
                for (int i = 0; (i < 4); i++) {
                    matrix[1][i] = 0.0;
                    for (int j = 0; (j < 4); j++) {
                        matrix[1][i] += a[i][j] * v[j];
                    }
                }
                break;
            }
        }
        return (matrix);
    }

    /*------------------------------------------------------------------*/
    private double getTranslationMeanSquares(
            final double[][] matrix
    ) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        xWeights();
        yWeights();
        if (outMsk == null) {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if (inMsk[yMsk + xMsk] != 0.0F) {
                                xIndexes();
                                area++;
                                difference = (double) outImg[k] - interpolate();
                                meanSquares += difference * difference;
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        } else {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
                                xIndexes();
                                area++;
                                difference = (double) outImg[k] - interpolate();
                                meanSquares += difference * difference;
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        }
        return (meanSquares / (double) area);
    }

    /*------------------------------------------------------------------*/
    private double getTranslationMeanSquares(
            final double[][] matrix,
            final double[] gradient
    ) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
        }
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        xWeights();
        yWeights();
        if (outMsk == null) {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if (inMsk[yMsk + xMsk] != 0.0F) {
                                area++;
                                xIndexes();
                                difference = (double) outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        } else {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
                                area++;
                                xIndexes();
                                difference = (double) outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        }
        return (meanSquares / (double) area);
    }

    /*------------------------------------------------------------------*/
    private double getTranslationMeanSquares(
            final double[][] matrix,
            final double[][] hessian,
            final double[] gradient
    ) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        double difference;
        double meanSquares = 0.0;
        long area = 0L;
        int xMsk;
        int yMsk;
        int k = 0;
        for (int i = 0; (i < transformation.getNativeValue()); i++) {
            gradient[i] = 0.0;
            for (int j = 0; (j < transformation.getNativeValue()); j++) {
                hessian[i][j] = 0.0;
            }
        }
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        xWeights();
        yWeights();
        if (outMsk == null) {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if (inMsk[yMsk + xMsk] != 0.0F) {
                                area++;
                                xIndexes();
                                difference = (double) outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                                hessian[0][0] += xGradient[k] * xGradient[k];
                                hessian[0][1] += xGradient[k] * yGradient[k];
                                hessian[1][1] += yGradient[k] * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        } else {
            for (int v = 0; (v < outNy); v++) {
                y = dy++;
                yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
                if ((0 <= yMsk) && (yMsk < inNy)) {
                    yMsk *= inNx;
                    yIndexes();
                    dx = dx0;
                    for (int u = 0; (u < outNx); u++, k++) {
                        x = dx++;
                        xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                        if ((0 <= xMsk) && (xMsk < inNx)) {
                            if ((outMsk[k] * inMsk[yMsk + xMsk]) != 0.0F) {
                                area++;
                                xIndexes();
                                difference = (double) outImg[k] - interpolate();
                                meanSquares += difference * difference;
                                gradient[0] += difference * xGradient[k];
                                gradient[1] += difference * yGradient[k];
                                hessian[0][0] += xGradient[k] * xGradient[k];
                                hessian[0][1] += xGradient[k] * yGradient[k];
                                hessian[1][1] += yGradient[k] * yGradient[k];
                            }
                        }
                    }
                } else {
                    k += outNx;
                }
            }
        }
        for (int i = 1; (i < transformation.getNativeValue()); i++) {
            for (int j = 0; (j < i); j++) {
                hessian[i][j] = hessian[j][i];
            }
        }
        return (meanSquares / (double) area);
    }

    /*------------------------------------------------------------------*/
    private double interpolate(
    ) {
        t = 0.0;
        for (int j = 0; (j < 4); j++) {
            s = 0.0;
            p = yIndex[j];
            for (int i = 0; (i < 4); i++) {
                s += xWeight[i] * (double) inImg[p + xIndex[i]];
            }
            t += yWeight[j] * s;
        }
        return (t);
    }

    /*------------------------------------------------------------------*/
    private double interpolateDx(
    ) {
        t = 0.0;
        for (int j = 0; (j < 4); j++) {
            s = 0.0;
            p = yIndex[j];
            for (int i = 0; (i < 4); i++) {
                s += dxWeight[i] * (double) inImg[p + xIndex[i]];
            }
            t += yWeight[j] * s;
        }
        return (t);
    }

    /*------------------------------------------------------------------*/
    private double interpolateDy(
    ) {
        t = 0.0;
        for (int j = 0; (j < 4); j++) {
            s = 0.0;
            p = yIndex[j];
            for (int i = 0; (i < 4); i++) {
                s += xWeight[i] * (double) inImg[p + xIndex[i]];
            }
            t += dyWeight[j] * s;
        }
        return (t);
    }

    /*------------------------------------------------------------------*/
    private void inverseMarquardtLevenbergOptimization(
            int workload
    ) {
        final double[][] attempt = new double[transformation.getNativeValue() / 2][2];
        final double[][] hessian = new double[transformation.getNativeValue()][transformation.getNativeValue()];
        final double[][] pseudoHessian = new double[transformation.getNativeValue()][transformation.getNativeValue()];
        final double[] gradient = new double[transformation.getNativeValue()];
        double[][] matrix = getTransformationMatrix(sourcePoint, targetPoint);
        double[] update = new double[transformation.getNativeValue()];
        double bestMeanSquares = 0.0;
        double meanSquares = 0.0;
        double lambda = FIRST_LAMBDA;
        double displacement;
        int iteration = 0;
        switch (transformation) {
            case Translation: {
                bestMeanSquares = getTranslationMeanSquares(
                        matrix, hessian, gradient);
                break;
            }
            case ScaledRotation: {
                bestMeanSquares = getScaledRotationMeanSquares(
                        sourcePoint, matrix, hessian, gradient);
                break;
            }
            case Affine: {
                bestMeanSquares = getAffineMeanSquares(
                        sourcePoint, matrix, hessian, gradient);
                break;
            }
        }
        iteration++;
        do {
            for (int k = 0; (k < transformation.getNativeValue()); k++) {
                pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
            }
            invertGauss(pseudoHessian);
            update = matrixMultiply(pseudoHessian, gradient);
            displacement = 0.0;
            for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                attempt[k][0] = sourcePoint[k][0] - update[2 * k];
                attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
                displacement += Math.sqrt(update[2 * k] * update[2 * k]
                        + update[2 * k + 1] * update[2 * k + 1]);
            }
            displacement /= 0.5 * (double) transformation.getNativeValue();
            matrix = getTransformationMatrix(attempt, targetPoint);
            switch (transformation) {
                case Translation: {
                    if (accelerated) {
                        meanSquares = getTranslationMeanSquares(
                                matrix, gradient);
                    } else {
                        meanSquares = getTranslationMeanSquares(
                                matrix, hessian, gradient);
                    }
                    break;
                }
                case ScaledRotation: {
                    if (accelerated) {
                        meanSquares = getScaledRotationMeanSquares(
                                attempt, matrix, gradient);
                    } else {
                        meanSquares = getScaledRotationMeanSquares(
                                attempt, matrix, hessian, gradient);
                    }
                    break;
                }
                case Affine: {
                    if (accelerated) {
                        meanSquares = getAffineMeanSquares(
                                attempt, matrix, gradient);
                    } else {
                        meanSquares = getAffineMeanSquares(
                                attempt, matrix, hessian, gradient);
                    }
                    break;
                }
            }
            iteration++;
            if (meanSquares < bestMeanSquares) {
                bestMeanSquares = meanSquares;
                for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                    sourcePoint[k][0] = attempt[k][0];
                    sourcePoint[k][1] = attempt[k][1];
                }
                lambda /= LAMBDA_MAGSTEP;
            } else {
                lambda *= LAMBDA_MAGSTEP;
            }

            workload--;
        } while ((iteration < (maxIterations * iterationPower - 1))
                && (pixelPrecision <= displacement));
        invertGauss(hessian);
        update = matrixMultiply(hessian, gradient);
        for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
            attempt[k][0] = sourcePoint[k][0] - update[2 * k];
            attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
        }
        matrix = getTransformationMatrix(attempt, targetPoint);
        switch (transformation) {
            case Translation: {
                meanSquares = getTranslationMeanSquares(matrix);
                break;
            }
            case ScaledRotation: {
                meanSquares = getScaledRotationMeanSquares(attempt, matrix);
                break;
            }
            case Affine: {
                meanSquares = getAffineMeanSquares(attempt, matrix);
                break;
            }
        }
        iteration++;
        if (meanSquares < bestMeanSquares) {
            for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                sourcePoint[k][0] = attempt[k][0];
                sourcePoint[k][1] = attempt[k][1];
            }
        }

    }

    /*------------------------------------------------------------------*/
    private void inverseMarquardtLevenbergRigidBodyOptimization(
            int workload
    ) {
        final double[][] attempt = new double[2][3];
        final double[][] hessian = new double[transformation.getNativeValue()][transformation.getNativeValue()];
        final double[][] pseudoHessian = new double[transformation.getNativeValue()][transformation.getNativeValue()];
        final double[] gradient = new double[transformation.getNativeValue()];
        double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        double[] update = new double[transformation.getNativeValue()];
        double bestMeanSquares = 0.0;
        double meanSquares = 0.0;
        double lambda = FIRST_LAMBDA;
        double angle;
        double c;
        double s;
        double displacement;
        int iteration = 0;
        for (int k = 0; (k < transformation.getNativeValue()); k++) {
            sourcePoint[k][0] = matrix[0][0] + targetPoint[k][0] * matrix[0][1]
                    + targetPoint[k][1] * matrix[0][2];
            sourcePoint[k][1] = matrix[1][0] + targetPoint[k][0] * matrix[1][1]
                    + targetPoint[k][1] * matrix[1][2];
        }
        matrix = getTransformationMatrix(sourcePoint, targetPoint);
        bestMeanSquares = getRigidBodyMeanSquares(matrix, hessian, gradient);
        iteration++;
        do {
            for (int k = 0; (k < transformation.getNativeValue()); k++) {
                pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
            }
            invertGauss(pseudoHessian);
            update = matrixMultiply(pseudoHessian, gradient);
            angle = Math.atan2(matrix[0][2], matrix[0][1]) - update[0];
            attempt[0][1] = Math.cos(angle);
            attempt[0][2] = Math.sin(angle);
            attempt[1][1] = -attempt[0][2];
            attempt[1][2] = attempt[0][1];
            c = Math.cos(update[0]);
            s = Math.sin(update[0]);
            attempt[0][0] = (matrix[0][0] + update[1]) * c
                    - (matrix[1][0] + update[2]) * s;
            attempt[1][0] = (matrix[0][0] + update[1]) * s
                    + (matrix[1][0] + update[2]) * c;
            displacement = Math.sqrt(update[1] * update[1] + update[2] * update[2])
                    + 0.25 * Math.sqrt((double) (inNx * inNx) + (double) (inNy * inNy))
                    * Math.abs(update[0]);
            if (accelerated) {
                meanSquares = getRigidBodyMeanSquares(attempt, gradient);
            } else {
                meanSquares = getRigidBodyMeanSquares(attempt, hessian, gradient);
            }
            iteration++;
            if (meanSquares < bestMeanSquares) {
                bestMeanSquares = meanSquares;
                for (int i = 0; (i < 2); i++) {
                    for (int j = 0; (j < 3); j++) {
                        matrix[i][j] = attempt[i][j];
                    }
                }
                lambda /= LAMBDA_MAGSTEP;
            } else {
                lambda *= LAMBDA_MAGSTEP;
            }

            workload--;
        } while ((iteration < (maxIterations * iterationPower - 1))
                && (pixelPrecision <= displacement));
        invertGauss(hessian);
        update = matrixMultiply(hessian, gradient);
        angle = Math.atan2(matrix[0][2], matrix[0][1]) - update[0];
        attempt[0][1] = Math.cos(angle);
        attempt[0][2] = Math.sin(angle);
        attempt[1][1] = -attempt[0][2];
        attempt[1][2] = attempt[0][1];
        c = Math.cos(update[0]);
        s = Math.sin(update[0]);
        attempt[0][0] = (matrix[0][0] + update[1]) * c
                - (matrix[1][0] + update[2]) * s;
        attempt[1][0] = (matrix[0][0] + update[1]) * s
                + (matrix[1][0] + update[2]) * c;
        meanSquares = getRigidBodyMeanSquares(attempt);
        iteration++;
        if (meanSquares < bestMeanSquares) {
            for (int i = 0; (i < 2); i++) {
                for (int j = 0; (j < 3); j++) {
                    matrix[i][j] = attempt[i][j];
                }
            }
        }
        for (int k = 0; (k < transformation.getNativeValue()); k++) {
            sourcePoint[k][0] = (targetPoint[k][0] - matrix[0][0]) * matrix[0][1]
                    + (targetPoint[k][1] - matrix[1][0]) * matrix[1][1];
            sourcePoint[k][1] = (targetPoint[k][0] - matrix[0][0]) * matrix[0][2]
                    + (targetPoint[k][1] - matrix[1][0]) * matrix[1][2];
        }

    }

    /*------------------------------------------------------------------*/
    private void invertGauss(
            final double[][] matrix
    ) {
        final int n = matrix.length;
        final double[][] inverse = new double[n][n];
        for (int i = 0; (i < n); i++) {
            double max = matrix[i][0];
            double absMax = Math.abs(max);
            for (int j = 0; (j < n); j++) {
                inverse[i][j] = 0.0;
                if (absMax < Math.abs(matrix[i][j])) {
                    max = matrix[i][j];
                    absMax = Math.abs(max);
                }
            }
            inverse[i][i] = 1.0 / max;
            for (int j = 0; (j < n); j++) {
                matrix[i][j] /= max;
            }
        }
        for (int j = 0; (j < n); j++) {
            double max = matrix[j][j];
            double absMax = Math.abs(max);
            int k = j;
            for (int i = j + 1; (i < n); i++) {
                if (absMax < Math.abs(matrix[i][j])) {
                    max = matrix[i][j];
                    absMax = Math.abs(max);
                    k = i;
                }
            }
            if (k != j) {
                final double[] partialLine = new double[n - j];
                final double[] fullLine = new double[n];
                System.arraycopy(matrix[j], j, partialLine, 0, n - j);
                System.arraycopy(matrix[k], j, matrix[j], j, n - j);
                System.arraycopy(partialLine, 0, matrix[k], j, n - j);
                System.arraycopy(inverse[j], 0, fullLine, 0, n);
                System.arraycopy(inverse[k], 0, inverse[j], 0, n);
                System.arraycopy(fullLine, 0, inverse[k], 0, n);
            }
            for (k = 0; (k <= j); k++) {
                inverse[j][k] /= max;
            }
            for (k = j + 1; (k < n); k++) {
                matrix[j][k] /= max;
                inverse[j][k] /= max;
            }
            for (int i = j + 1; (i < n); i++) {
                for (k = 0; (k <= j); k++) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
                for (k = j + 1; (k < n); k++) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }
        for (int j = n - 1; (1 <= j); j--) {
            for (int i = j - 1; (0 <= i); i--) {
                for (int k = 0; (k <= j); k++) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
                for (int k = j + 1; (k < n); k++) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }
        for (int i = 0; (i < n); i++) {
            System.arraycopy(inverse[i], 0, matrix[i], 0, n);
        }
    }

    /*------------------------------------------------------------------*/
    private void MarquardtLevenbergOptimization(
            int workload
    ) {
        final double[][] attempt = new double[transformation.getNativeValue() / 2][2];
        final double[][] hessian = new double[transformation.getNativeValue()][transformation.getNativeValue()];
        final double[][] pseudoHessian = new double[transformation.getNativeValue()][transformation.getNativeValue()];
        final double[] gradient = new double[transformation.getNativeValue()];
        double[][] matrix = getTransformationMatrix(targetPoint, sourcePoint);
        double[] update = new double[transformation.getNativeValue()];
        double bestMeanSquares = 0.0;
        double meanSquares = 0.0;
        double lambda = FIRST_LAMBDA;
        double displacement;
        int iteration = 0;
        bestMeanSquares = getBilinearMeanSquares(matrix, hessian, gradient);
        iteration++;
        do {
            for (int k = 0; (k < transformation.getNativeValue()); k++) {
                pseudoHessian[k][k] = (1.0 + lambda) * hessian[k][k];
            }
            invertGauss(pseudoHessian);
            update = matrixMultiply(pseudoHessian, gradient);
            displacement = 0.0;
            for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                attempt[k][0] = sourcePoint[k][0] - update[2 * k];
                attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
                displacement += Math.sqrt(update[2 * k] * update[2 * k]
                        + update[2 * k + 1] * update[2 * k + 1]);
            }
            displacement /= 0.5 * (double) transformation.getNativeValue();
            matrix = getTransformationMatrix(targetPoint, attempt);
            meanSquares = getBilinearMeanSquares(matrix, hessian, gradient);
            iteration++;
            if (meanSquares < bestMeanSquares) {
                bestMeanSquares = meanSquares;
                for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                    sourcePoint[k][0] = attempt[k][0];
                    sourcePoint[k][1] = attempt[k][1];
                }
                lambda /= LAMBDA_MAGSTEP;
            } else {
                lambda *= LAMBDA_MAGSTEP;
            }

            workload--;
        } while ((iteration < (maxIterations * iterationPower - 1))
                && (pixelPrecision <= displacement));
        invertGauss(hessian);
        update = matrixMultiply(hessian, gradient);
        for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
            attempt[k][0] = sourcePoint[k][0] - update[2 * k];
            attempt[k][1] = sourcePoint[k][1] - update[2 * k + 1];
        }
        matrix = getTransformationMatrix(targetPoint, attempt);
        meanSquares = getBilinearMeanSquares(matrix);
        iteration++;
        if (meanSquares < bestMeanSquares) {
            for (int k = 0; (k < (transformation.getNativeValue() / 2)); k++) {
                sourcePoint[k][0] = attempt[k][0];
                sourcePoint[k][1] = attempt[k][1];
            }
        }

    }

    /*------------------------------------------------------------------*/
    private double[] matrixMultiply(
            final double[][] matrix,
            final double[] vector
    ) {
        final double[] result = new double[matrix.length];
        for (int i = 0; (i < matrix.length); i++) {
            result[i] = 0.0;
            for (int j = 0; (j < vector.length); j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return (result);
    }

    /*------------------------------------------------------------------*/
    private void scaleBottomDownLandmarks(
    ) {
        for (int depth = 1; (depth < pyramidDepth); depth++) {
            if (transformation == TurboRegTransformationType.RigidBody) {
                for (int n = 0; (n < transformation.getNativeValue()); n++) {
                    sourcePoint[n][0] *= 0.5;
                    sourcePoint[n][1] *= 0.5;
                    targetPoint[n][0] *= 0.5;
                    targetPoint[n][1] *= 0.5;
                }
            } else {
                for (int n = 0; (n < (transformation.getNativeValue() / 2)); n++) {
                    sourcePoint[n][0] *= 0.5;
                    sourcePoint[n][1] *= 0.5;
                    targetPoint[n][0] *= 0.5;
                    targetPoint[n][1] *= 0.5;
                }
            }
        }
    }

    /*------------------------------------------------------------------*/
    private void scaleUpLandmarks(
    ) {
        if (transformation == TurboRegTransformationType.RigidBody) {
            for (int n = 0; (n < transformation.getNativeValue()); n++) {
                sourcePoint[n][0] *= 2.0;
                sourcePoint[n][1] *= 2.0;
                targetPoint[n][0] *= 2.0;
                targetPoint[n][1] *= 2.0;
            }
        } else {
            for (int n = 0; (n < (transformation.getNativeValue() / 2)); n++) {
                sourcePoint[n][0] *= 2.0;
                sourcePoint[n][1] *= 2.0;
                targetPoint[n][0] *= 2.0;
                targetPoint[n][1] *= 2.0;
            }
        }
    }

    /*------------------------------------------------------------------*/
    private void translationTransform(
            final double[][] matrix
    ) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        int xMsk;
        int yMsk;
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        if (!accelerated) {
            xWeights();
            yWeights();
        }
        int k = 0;

        for (int v = 0; (v < outNy); v++) {
            y = dy++;
            yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
            if ((0 <= yMsk) && (yMsk < inNy)) {
                yMsk *= inNx;
                if (!accelerated) {
                    yIndexes();
                }
                dx = dx0;
                for (int u = 0; (u < outNx); u++) {
                    x = dx++;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)) {
                        xMsk += yMsk;
                        if (accelerated) {
                            outImg[k++] = inImg[xMsk];
                        } else {
                            xIndexes();
                            outImg[k++] = (float) interpolate();
                        }
                    } else {
                        outImg[k++] = 0.0F;
                    }
                }
            } else {
                for (int u = 0; (u < outNx); u++) {
                    outImg[k++] = 0.0F;
                }
            }

        }

    } /* translationTransform */

    /*------------------------------------------------------------------*/
    private void translationTransform(
            final double[][] matrix,
            final float[] outMsk
    ) {
        double dx = matrix[0][0];
        double dy = matrix[1][0];
        final double dx0 = dx;
        int xMsk;
        int yMsk;
        x = dx - Math.floor(dx);
        y = dy - Math.floor(dy);
        if (!accelerated) {
            xWeights();
            yWeights();
        }
        int k = 0;

        for (int v = 0; (v < outNy); v++) {
            y = dy++;
            yMsk = (0.0 <= y) ? ((int) (y + 0.5)) : ((int) (y - 0.5));
            if ((0 <= yMsk) && (yMsk < inNy)) {
                yMsk *= inNx;
                if (!accelerated) {
                    yIndexes();
                }
                dx = dx0;
                for (int u = 0; (u < outNx); u++, k++) {
                    x = dx++;
                    xMsk = (0.0 <= x) ? ((int) (x + 0.5)) : ((int) (x - 0.5));
                    if ((0 <= xMsk) && (xMsk < inNx)) {
                        xMsk += yMsk;
                        if (accelerated) {
                            outImg[k] = inImg[xMsk];
                        } else {
                            xIndexes();
                            outImg[k] = (float) interpolate();
                        }
                        outMsk[k] = inMsk[xMsk];
                    } else {
                        outImg[k] = 0.0F;
                        outMsk[k] = 0.0F;
                    }
                }
            } else {
                for (int u = 0; (u < outNx); u++, k++) {
                    outImg[k] = 0.0F;
                    outMsk[k] = 0.0F;
                }
            }

        }

    } /* translationTransform */

    /*------------------------------------------------------------------*/
    private void xDxWeights(
    ) {
        s = 1.0 - x;
        dxWeight[0] = 0.5 * x * x;
        xWeight[0] = x * dxWeight[0] / 3.0;
        dxWeight[3] = -0.5 * s * s;
        xWeight[3] = s * dxWeight[3] / -3.0;
        dxWeight[1] = 1.0 - 2.0 * dxWeight[0] + dxWeight[3];
        xWeight[1] = 2.0 / 3.0 + (1.0 + x) * dxWeight[3];
        dxWeight[2] = 1.5 * x * (x - 4.0 / 3.0);
        xWeight[2] = 2.0 / 3.0 - (2.0 - x) * dxWeight[0];
    } /* xDxWeights */

    /*------------------------------------------------------------------*/
    private void xIndexes(
    ) {
        p = (0.0 <= x) ? ((int) x + 2) : ((int) x + 1);
        for (int k = 0; (k < 4); p--, k++) {
            q = (p < 0) ? (-1 - p) : (p);
            if (twiceInNx <= q) {
                q -= twiceInNx * (q / twiceInNx);
            }
            xIndex[k] = (inNx <= q) ? (twiceInNx - 1 - q) : (q);
        }
    } /* xIndexes */

    /*------------------------------------------------------------------*/
    private void xWeights(
    ) {
        s = 1.0 - x;
        xWeight[3] = s * s * s / 6.0;
        s = x * x;
        xWeight[2] = 2.0 / 3.0 - 0.5 * s * (2.0 - x);
        xWeight[0] = s * x / 6.0;
        xWeight[1] = 1.0 - xWeight[0] - xWeight[2] - xWeight[3];
    } /* xWeights */

    /*------------------------------------------------------------------*/
    private void yDyWeights(
    ) {
        t = 1.0 - y;
        dyWeight[0] = 0.5 * y * y;
        yWeight[0] = y * dyWeight[0] / 3.0;
        dyWeight[3] = -0.5 * t * t;
        yWeight[3] = t * dyWeight[3] / -3.0;
        dyWeight[1] = 1.0 - 2.0 * dyWeight[0] + dyWeight[3];
        yWeight[1] = 2.0 / 3.0 + (1.0 + y) * dyWeight[3];
        dyWeight[2] = 1.5 * y * (y - 4.0 / 3.0);
        yWeight[2] = 2.0 / 3.0 - (2.0 - y) * dyWeight[0];
    } /* yDyWeights */

    /*------------------------------------------------------------------*/
    private void yIndexes(
    ) {
        p = (0.0 <= y) ? ((int) y + 2) : ((int) y + 1);
        for (int k = 0; (k < 4); p--, k++) {
            q = (p < 0) ? (-1 - p) : (p);
            if (twiceInNy <= q) {
                q -= twiceInNy * (q / twiceInNy);
            }
            yIndex[k] = (inNy <= q) ? ((twiceInNy - 1 - q) * inNx) : (q * inNx);
        }
    } /* yIndexes */

    /*------------------------------------------------------------------*/
    private void yWeights(
    ) {
        t = 1.0 - y;
        yWeight[3] = t * t * t / 6.0;
        t = y * y;
        yWeight[2] = 2.0 / 3.0 - 0.5 * t * (2.0 - y);
        yWeight[0] = t * y / 6.0;
        yWeight[1] = 1.0 - yWeight[0] - yWeight[2] - yWeight[3];
    } /* yWeights */

}