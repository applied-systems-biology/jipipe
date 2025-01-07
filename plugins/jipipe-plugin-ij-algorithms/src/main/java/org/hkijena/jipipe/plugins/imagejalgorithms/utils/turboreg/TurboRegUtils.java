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
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg.AdvancedTurboRegParameters;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

public class TurboRegUtils {

    public static TurboRegResult alignImage2D(ImagePlus source, ImagePlus target, TurboRegTransformationType transformationType, AdvancedTurboRegParameters advancedTurboRegParameters) {

        // Full image crop (from MultiStackReg)
        final int[] sourceCrop = new int[] {
                0, 0, source.getWidth() - 1, source.getHeight() - 1
        };
        final int[] targetCrop = new int[] {
                0, 0, source.getWidth() - 1, source.getHeight() - 1
        };

        source.setRoi(sourceCrop[0], sourceCrop[1], sourceCrop[2], sourceCrop[3]);
        target.setRoi(targetCrop[0], targetCrop[1], targetCrop[2], targetCrop[3]);
        source.setSlice(1);
        target.setSlice(1);

        final ImagePlus sourceImp = new ImagePlus("source",
                source.getProcessor().crop());
        final ImagePlus targetImp = new ImagePlus("target",
                target.getProcessor().crop());

//        sourceImp.show();
//        targetImp.show();

        double[][] sourcePoints =
                new double[TurboRegPointHandler.NUM_POINTS][2];
        double[][] targetPoints =
                new double[TurboRegPointHandler.NUM_POINTS][2];

        TurboRegUtils.initializeSourceAndTargetPoints(source, sourcePoints, targetPoints, transformationType);

        final TurboRegImage sourceImg = new TurboRegImage(
                sourceImp, transformationType, false);
        final TurboRegImage targetImg = new TurboRegImage(
                targetImp, transformationType, true);
        final int pyramidDepth = TurboRegUtils.getPyramidDepth(
                sourceImp.getWidth(), sourceImp.getHeight(),
                targetImp.getWidth(), targetImp.getHeight(), advancedTurboRegParameters.getMinSize());

        sourceImg.setPyramidDepth(pyramidDepth);
        targetImg.setPyramidDepth(pyramidDepth);

        // TODO: progress
        sourceImg.run();
        targetImg.run();

        if (2 <= source.getStackSize()) {
            source.setSlice(2);
        }
        if (2 <= target.getStackSize()) {
            target.setSlice(2);
        }

        // Mask generation
        final ImagePlus sourceMskImp = new ImagePlus("source mask",
                source.getProcessor().crop());
        final ImagePlus targetMskImp = new ImagePlus("target mask",
                target.getProcessor().crop());
        final TurboRegMask sourceMsk = new TurboRegMask(sourceMskImp);
        final TurboRegMask targetMsk = new TurboRegMask(targetMskImp);

        source.setSlice(1);
        target.setSlice(1);
        if (source.getStackSize() < 2) {
            sourceMsk.clearMask();
        }
        if (target.getStackSize() < 2) {
            targetMsk.clearMask();
        }

        sourceMsk.setPyramidDepth(pyramidDepth);
        targetMsk.setPyramidDepth(pyramidDepth);

        // TODO: progress
        sourceMsk.run();
        targetMsk.run();

        // Handling crop
        switch (transformationType) {
            case Translation: {
                sourcePoints[0][0] -= sourceCrop[0];
                sourcePoints[0][1] -= sourceCrop[1];
                targetPoints[0][0] -= targetCrop[0];
                targetPoints[0][1] -= targetCrop[1];
                break;
            }
            case ScaledRotation: {
                for (int k = 0; (k < 2); k++) {
                    sourcePoints[k][0] -= sourceCrop[0];
                    sourcePoints[k][1] -= sourceCrop[1];
                    targetPoints[k][0] -= targetCrop[0];
                    targetPoints[k][1] -= targetCrop[1];
                }
                break;
            }
            case RigidBody:
            case Affine: {
                for (int k = 0; (k < 3); k++) {
                    sourcePoints[k][0] -= sourceCrop[0];
                    sourcePoints[k][1] -= sourceCrop[1];
                    targetPoints[k][0] -= targetCrop[0];
                    targetPoints[k][1] -= targetCrop[1];
                }
                break;
            }
            case Bilinear: {
                for (int k = 0; (k < 4); k++) {
                    sourcePoints[k][0] -= sourceCrop[0];
                    sourcePoints[k][1] -= sourceCrop[1];
                    targetPoints[k][0] -= targetCrop[0];
                    targetPoints[k][1] -= targetCrop[1];
                }
                break;
            }
        }

        // Update the landmark points using the point handler
        final TurboRegPointHandler sourcePh = new TurboRegPointHandler(
                sourceImp, transformationType);
        final TurboRegPointHandler targetPh = new TurboRegPointHandler(
                targetImp, transformationType);
        sourcePh.setPoints(sourcePoints);
        targetPh.setPoints(targetPoints);

        // Do the registration
        {
            TurboRegTransformer transformer = new TurboRegTransformer(sourceImg,
                    sourceMsk,
                    sourcePh,
                    targetImg,
                    targetMsk,
                    targetPh,
                    transformationType,
                    false,
                    false);
            transformer.doRegistration();
        }

        // Grab the updated points
        sourcePoints = sourcePh.getPoints();
        targetPoints = targetPh.getPoints();

        // Generate transformation info
        TurboRegTransformationInfo transformation = new TurboRegTransformationInfo();
        transformation.getEntries().add(new TurboRegTransformationInfo.Entry(new ImageSliceIndex(),
                new ImageSliceIndex(),
                transformationType,
                sourcePoints,
                targetPoints));

        // TODO: refined landmarks?
        source.killRoi();
        target.killRoi();

        ImagePlus transformedImage = TurboRegUtils.transformImage2D(source, target.getWidth(), target.getHeight(), transformationType, sourcePoints, targetPoints);

        TurboRegResult finalResult = new TurboRegResult();
        finalResult.setTransformation(transformation);
        finalResult.setTransformedTargetImage(transformedImage);

        return finalResult;
    }

    public static int getPyramidDepth(int sw, int sh, int tw, int th, int minSize) {
        int pyramidDepth = 1;
        while (((2 * minSize) <= sw)
                && ((2 * minSize) <= sh)
                && ((2 * minSize) <= tw)
                && ((2 * minSize) <= th)) {
            sw /= 2;
            sh /= 2;
            tw /= 2;
            th /= 2;
            pyramidDepth++;
        }
        return (pyramidDepth);
    }

    public static void initializeSourceAndTargetPoints(ImagePlus source, double[][] sourcePoints, double[][] targetPoints, TurboRegTransformationType transformationType) {
        final int width = source.getWidth();
        final int height = source.getHeight();
        if (transformationType == TurboRegTransformationType.RigidBody) {
            sourcePoints[0][0] = (width / 2);
            sourcePoints[0][1] = (height / 2);
            targetPoints[0][0] = (width / 2);
            targetPoints[0][1] = (height / 2);
            sourcePoints[1][0] = (width / 2);
            sourcePoints[1][1] = (height / 4);
            targetPoints[1][0] = (width / 2);
            targetPoints[1][1] = (height / 4);
            sourcePoints[2][0] = (width / 2);
            sourcePoints[2][1] = ((3 * height) / 4);
            targetPoints[2][0] = (width / 2);
            targetPoints[2][1] = ((3 * height) / 4);
        } else if (transformationType == TurboRegTransformationType.Affine) {
            sourcePoints[0][0] = (width / 2);
            sourcePoints[0][1] = (height / 4);
            targetPoints[0][0] = (width / 2);
            targetPoints[0][1] = (height / 4);
            sourcePoints[1][0] = (width / 4);
            sourcePoints[1][1] = ((3 * height) / 4);
            targetPoints[1][0] = (width / 4);
            targetPoints[1][1] = ((3 * height) / 4);
            sourcePoints[2][0] = ((3 * width) / 4);
            sourcePoints[2][1] = ((3 * height) / 4);
            targetPoints[2][0] = ((3 * width) / 4);
            targetPoints[2][1] = ((3 * height) / 4);
        } else if (transformationType == TurboRegTransformationType.Translation) {
            sourcePoints[0][0] = (width / 2);
            sourcePoints[0][1] = (height / 2);
            targetPoints[0][0] = (width / 2);
            targetPoints[0][1] = (height / 2);
        } else if (transformationType == TurboRegTransformationType.ScaledRotation) {
            sourcePoints[0][0] = (width / 4);
            sourcePoints[0][1] = (height / 2);
            targetPoints[0][0] = (width / 4);
            targetPoints[0][1] = (height / 2);
            sourcePoints[1][0] = ((3 * width) / 4);
            sourcePoints[1][1] = (height / 2);
            targetPoints[1][0] = ((3 * width) / 4);
            targetPoints[1][1] = (height / 2);
        } else if (transformationType == TurboRegTransformationType.Bilinear) {
            throw new UnsupportedOperationException("Bilinear not supported yet.");
        }
    }

    public static ImagePlus transformImage2D(
            final ImagePlus source,
            final int width,
            final int height,
            final TurboRegTransformationType transformation,
            double[][] sourcePoints,
            double[][] targetPoints) {
        if ((source.getType() != ImagePlus.GRAY16)
                && (source.getType() != ImagePlus.GRAY32)
                && ((source.getType() != ImagePlus.GRAY8)
                || source.getStack().isRGB() || source.getStack().isHSB())) {
            throw new RuntimeException("Unsupported image type: " + source.getType());
        }
        source.setSlice(1);
        final TurboRegImage sourceImg = new TurboRegImage(source,
                TurboRegTransformationType.GenericTransformation, false);
        sourceImg.run();
        if (2 <= source.getStackSize()) {
            source.setSlice(2);
        }
        final TurboRegMask sourceMsk = new TurboRegMask(source);
        source.setSlice(1);
        if (source.getStackSize() < 2) {
            sourceMsk.clearMask();
        }
        final TurboRegPointHandler sourcePh = new TurboRegPointHandler(
                sourcePoints, transformation);
        final TurboRegPointHandler targetPh = new TurboRegPointHandler(
                targetPoints, transformation);

        final TurboRegTransformer regTransform = new TurboRegTransformer(
                sourceImg, sourceMsk, sourcePh,
                null, null, targetPh, transformation, false, false);
        ImagePlus transformedImage = regTransform.doFinalTransform(width, height);

        // Postprocessing
        transformedImage.getStack().deleteLastSlice();
        if (source.getType() == ImagePlus.GRAY8) {
            transformedImage.getProcessor().setMinAndMax(0, 255);
            transformedImage = ImageJUtils.convertToGreyscale8UIfNeeded(transformedImage);
        } else if (source.getType() == ImagePlus.GRAY16) {
            transformedImage.getProcessor().setMinAndMax(0, 65535);
            transformedImage = ImageJUtils.convertToGrayscale16UIfNeeded(transformedImage);
        }

        return transformedImage;
    }
}
