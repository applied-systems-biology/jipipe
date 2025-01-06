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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.strings.XMLData;

/**
 * Port of {@link register_virtual_stack.Register_Virtual_Stack_MT}
 */
@SetJIPipeDocumentation(name = "TurboReg registration", description = "Aligns or to matches two images, one of them being called the source image and the other the target image.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Registration")
@AddJIPipeCitation("Based on TurboReg")
@AddJIPipeCitation("Based on MultiStackReg")
@AddJIPipeCitation("https://bigwww.epfl.ch/thevenaz/turboreg/")
@AddJIPipeCitation("https://github.com/miura/MultiStackRegistration/")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Reference", description = "The reference image", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Target", description = "The target image that will be registered to the reference", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Reference", description = "The reference image", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Target", description = "The registered image", create = true)
@AddJIPipeOutputSlot(value = XMLData.class, name = "Transform", description = "The transform function in TrakEM format", create = true)
public class TurboRegRegistrationAlgorithm extends JIPipeIteratingAlgorithm {
    
    private TurboRegTransformation transformation = TurboRegTransformation.RigidBody;

    /**
     * Minimal linear dimension of an image in the multiresolution pyramid.
     */
    private int minSize = 12;


    public TurboRegRegistrationAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TurboRegRegistrationAlgorithm(TurboRegRegistrationAlgorithm other) {
        super(other);
        this.transformation = other.transformation;
    }



    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus source = iterationStep.getInputData("Reference", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();
        ImagePlus target = iterationStep.getInputData("Target", ImagePlusGreyscaleData.class, progressInfo).getDuplicateImage();

        transformation = TurboRegTransformation.RigidBody;

        if(!ImageJUtils.imagesHaveSameSize(source, target)) {
            throw new RuntimeException("Source and target images do not have the same size!");
        }

        // TODO: handle stacks etc.
        final ImagePlus sourceImp = new ImagePlus("source",
                source.getProcessor().crop());
        final ImagePlus targetImp = new ImagePlus("target",
                target.getProcessor().crop());

        double[][] sourcePoints =
                new double[TurboRegPointHandler.NUM_POINTS][2];
        double[][] targetPoints =
                new double[TurboRegPointHandler.NUM_POINTS][2];

        if(transformation == TurboRegTransformation.RigidBody) {
            final int width = source.getWidth();
            final int height = source.getHeight();
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
        }

        final TurboRegImage sourceImg = new TurboRegImage(
                sourceImp, transformation, false);
        final TurboRegImage targetImg = new TurboRegImage(
                targetImp, transformation, true);
        final int pyramidDepth = getPyramidDepth(
                sourceImp.getWidth(), sourceImp.getHeight(),
                targetImp.getWidth(), targetImp.getHeight());

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

        // Point handler
        final TurboRegPointHandler sourcePh = new TurboRegPointHandler(
                sourceImp, transformation);
        final TurboRegPointHandler targetPh = new TurboRegPointHandler(
                targetImp, transformation);
        sourcePh.setPoints(sourcePoints);
        targetPh.setPoints(targetPoints);

        // TODO: FinalAction???

        sourcePoints = sourcePh.getPoints();
        targetPoints = targetPh.getPoints();

        // TODO: refined landmarks?
        source.killRoi();
        target.killRoi();

        ImagePlus result = transformImage(source, target.getWidth(), target.getHeight(), transformation, sourcePoints, targetPoints);

        iterationStep.addOutputData("Target", new ImagePlusData(result), progressInfo);

        System.out.println();

    }

    private ImagePlus transformImage (
            final ImagePlus source,
            final int width,
            final int height,
            final TurboRegTransformation transformation,
            double[][] sourcePoints, double[][] targetPoints
    ) {
        if ((source.getType() != ImagePlus.GRAY16)
                && (source.getType() != ImagePlus.GRAY32)
                && ((source.getType() != ImagePlus.GRAY8)
                || source.getStack().isRGB() || source.getStack().isHSB())) {
            IJ.error(
                    source.getTitle() + " should be grayscale (8, 16, or 32 bit)");
            return(null);
        }
        source.setSlice(1);
        final TurboRegImage sourceImg = new TurboRegImage(source,
                TurboRegTransformation.GenericTransformation, false);
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

        final TurboRegTransform regTransform = new TurboRegTransform(
                sourceImg, sourceMsk, sourcePh,
                null, null, targetPh, transformation, false, false);
        final ImagePlus transformedImage = regTransform.doFinalTransform(
                width, height);
//        if (false) {
//            transformedImage.setSlice(1);
//            transformedImage.getProcessor().resetMinAndMax();
//            transformedImage.show();
//            transformedImage.updateAndDraw();
//        }
        return transformedImage;
    }

    private int getPyramidDepth (int sw, int sh, int tw, int th) {
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
        return(pyramidDepth);
    }
}
