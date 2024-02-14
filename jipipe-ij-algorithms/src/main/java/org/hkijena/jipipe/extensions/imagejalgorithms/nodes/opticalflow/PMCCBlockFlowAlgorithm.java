package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.opticalflow;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import mpicbg.ij.integral.BlockPMCC;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale.ImagePlus4DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

import java.util.Arrays;

/**
 * Adapted from {@link mpicbg.ij.plugin.PMCCBlockFlow}, as the methods there are all protected/private
 */
@JIPipeDocumentation(name = "Optical flow (PMCC Block MSE)", description = "Transfers image sequences into an optic flow field.\n" +
        "Flow fields are calculated for each pair (t,t+1) of the sequence with length |T| independently. " +
        "The motion vector for each pixel in image t is estimated by searching the most similar looking pixel in image t+1. " +
        "The similarity measure is Pearson Product-Moment Correlation Coefficient of all pixels in a local vicinity. " +
        "The local vicinity is defined by a block and is calculated using an IntegralImage. " +
        "Both the size of the block and the search radius are parameters of the method.\n\n" +
        "The output is a two-channel image with (T-1) items. The pixels in each channel describe the relative location" +
        " of the next similar pixel in polar coordinates (default) or cartesian coordinates.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Optical flow")
@JIPipeInputSlot(value = ImagePlus3DGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus4DGreyscale32FData.class, slotName = "Vector field", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nOptic Flow", aliasName = "Integral Block PMCC")
public class PMCCBlockFlowAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int blockRadius = 8;
    private int maxDistance = 7;
    private boolean outputPolarCoordinates = true;
    private boolean relativeDistances = true;
    private boolean addLastIdentityField = false;

    public PMCCBlockFlowAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public PMCCBlockFlowAlgorithm(PMCCBlockFlowAlgorithm other) {
        super(other);
        this.blockRadius = other.blockRadius;
        this.maxDistance = other.maxDistance;
        this.outputPolarCoordinates = other.outputPolarCoordinates;
        this.relativeDistances = other.relativeDistances;
        this.addLastIdentityField = other.addLastIdentityField;
    }

    @JIPipeDocumentation(name = "Add identity field at end", description = "If enabled, the output will contain as many planes as the input. " +
            "The last slide will map to identity (zero image)")
    @JIPipeParameter("add-last-identity-field")
    public boolean isAddLastIdentityField() {
        return addLastIdentityField;
    }

    @JIPipeParameter("add-last-identity-field")
    public void setAddLastIdentityField(boolean addLastIdentityField) {
        this.addLastIdentityField = addLastIdentityField;
    }

    @JIPipeDocumentation(name = "Relative distances", description = "If enabled, the output radius or x/y are relative to the max distance.")
    @JIPipeParameter("relative-distances")
    public boolean isRelativeDistances() {
        return relativeDistances;
    }

    @JIPipeParameter("relative-distances")
    public void setRelativeDistances(boolean relativeDistances) {
        this.relativeDistances = relativeDistances;
    }

    @JIPipeDocumentation(name = "Block radius", description = "Determines the local vicinity that is used to calculate the similarity of two pixels.")
    @JIPipeParameter("block-radius")
    public int getBlockRadius() {
        return blockRadius;
    }

    @JIPipeParameter("block-radius")
    public boolean setBlockRadius(int blockRadius) {
        if (blockRadius <= 0)
            return false;
        this.blockRadius = blockRadius;
        return true;
    }

    @JIPipeDocumentation(name = "Max distance", description = "Maximum search distance for a most similar pixel. The maximum value is 127 due to performance reasons.")
    @JIPipeParameter("max-distance")
    public int getMaxDistance() {
        return maxDistance;
    }

    @JIPipeParameter("max-distance")
    public boolean setMaxDistance(int maxDistance) {
        if (maxDistance <= 0 || maxDistance >= 127)
            return false;
        this.maxDistance = maxDistance;
        return true;
    }

    @JIPipeDocumentation(name = "Output polar coordinates", description = "If enabled, the output contains polar coordinates (channel 0 being the radius and channel 1 being phi). " +
            "If disabled, the output contains cartesian coordinates.")
    @JIPipeParameter("output-polar-coordinates")
    public boolean isOutputPolarCoordinates() {
        return outputPolarCoordinates;
    }

    @JIPipeParameter("output-polar-coordinates")
    public void setOutputPolarCoordinates(boolean outputPolarCoordinates) {
        this.outputPolarCoordinates = outputPolarCoordinates;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = iterationStep.getInputData(getFirstInputSlot(), ImagePlus3DGreyscale32FData.class, progressInfo).getImage();
        ImageStack seq = imp.getStack();
        int outputSize = 2 * seq.getSize() - 2;
        if (addLastIdentityField)
            outputSize += 2;
        ImageStack seqFlowVectors = new ImageStack(imp.getWidth(), imp.getHeight(), outputSize);

        FloatProcessor ip1;
        FloatProcessor ip2 = (FloatProcessor) seq.getProcessor(1).convertToFloat();

        CompositeImage impFlowVectors = null;

        for (int i = 1; i < seq.getSize(); ++i) {
            progressInfo.resolveAndLog("Slice", i, seq.getSize());
            ip1 = ip2;
            ip2 = (FloatProcessor) seq.getProcessor(i + 1).convertToFloat();

            final FloatProcessor seqFlowVectorRSlice = new FloatProcessor(imp.getWidth(), imp.getHeight());
            final FloatProcessor seqFlowVectorPhiSlice = new FloatProcessor(imp.getWidth(), imp.getHeight());

            opticFlow(ip1, ip2, seqFlowVectorRSlice, seqFlowVectorPhiSlice);

            seqFlowVectors.setPixels(seqFlowVectorRSlice.getPixels(), 2 * i - 1);
            seqFlowVectors.setSliceLabel("r " + i, 2 * i - 1);
            seqFlowVectors.setPixels(seqFlowVectorPhiSlice.getPixels(), 2 * i);
            seqFlowVectors.setSliceLabel("phi " + i, 2 * i);

            if (i == 1) {
                final ImagePlus notYetComposite = new ImagePlus(imp.getTitle() + " flow vectors", seqFlowVectors);
                notYetComposite.setOpenAsHyperStack(true);
                notYetComposite.setCalibration(imp.getCalibration());
                notYetComposite.setDimensions(2, 1, seq.getSize() - 1);

                impFlowVectors = new CompositeImage(notYetComposite, CompositeImage.GRAYSCALE);
                impFlowVectors.setOpenAsHyperStack(true);
                impFlowVectors.setDimensions(2, 1, seq.getSize() - 1);

                if (outputPolarCoordinates) {
                    impFlowVectors.setPosition(1, 1, 1);
                    impFlowVectors.setDisplayRange(0, 1);
                    impFlowVectors.setPosition(2, 1, 1);
                    impFlowVectors.setDisplayRange(-Math.PI, Math.PI);
                }
            }
            impFlowVectors.setPosition(1, 1, i);
            imp.setSlice(i + 1);
        }

        if (addLastIdentityField) {
            seqFlowVectors.setPixels(new float[imp.getWidth() * imp.getHeight()], 2 * seq.getSize());
            seqFlowVectors.setPixels(new float[imp.getWidth() * imp.getHeight()], 2 * seq.getSize() - 1);
            impFlowVectors.setDimensions(2, 1, seq.getSize());
        }

        if (!outputPolarCoordinates) {
            ImageJUtils.calibrate(impFlowVectors, ImageJCalibrationMode.AutomaticImageJ, 0, 0);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus4DGreyscale32FData(impFlowVectors), progressInfo);
    }

    public void opticFlow(
            final FloatProcessor ip1,
            final FloatProcessor ip2,
            final FloatProcessor r,
            final FloatProcessor phi) {
        final BlockPMCC bc = new BlockPMCC(ip1.getWidth(), ip1.getHeight(), ip1, ip2);
        //final BlockPMCC bc = new BlockPMCC( ip1, ip2 );

        final FloatProcessor ipR = bc.getTargetProcessor();
        final ColorProcessor ipX = new ColorProcessor(ipR.getWidth(), ipR.getHeight());
        final ColorProcessor ipY = new ColorProcessor(ipR.getWidth(), ipR.getHeight());

        /* init */
        {
            final float[] ipRMaxPixels = (float[]) r.getPixels();
            Arrays.fill(ipRMaxPixels, -1);
        }

        for (int yo = -maxDistance; yo <= maxDistance; ++yo) {
            for (int xo = -maxDistance; xo <= maxDistance; ++xo) {
                // continue if radius is larger than maxDistance
                if (yo * yo + xo * xo > maxDistance * maxDistance) continue;

                bc.setOffset(xo, yo);
                bc.rSignedSquare(blockRadius);

//				stack.addSlice( xo + " " + yo, ipR.duplicate() );

                final float[] ipRPixels = (float[]) ipR.getPixels();
                final float[] ipRMaxPixels = (float[]) r.getPixels();
                final int[] ipXPixels = (int[]) ipX.getPixels();
                final int[] ipYPixels = (int[]) ipY.getPixels();

                // update the translation fields
                final int h = ipR.getHeight() - maxDistance;
                final int width = ipR.getWidth();
                final int w = width - maxDistance;
                for (int y = maxDistance; y < h; ++y) {
                    final int row = y * width;
                    final int rowR;
                    if (yo < 0)
                        rowR = row;
                    else
                        rowR = (y - yo) * width;
                    for (int x = maxDistance; x < w; ++x) {
                        final int i = row + x;
                        final int iR;
                        if (xo < 0)
                            iR = rowR + x;
                        else
                            iR = rowR + (x - xo);

                        final float ipRPixel = ipRPixels[iR];
                        final float ipRMaxPixel = ipRMaxPixels[i];

                        if (ipRPixel > ipRMaxPixel) {
                            ipRMaxPixels[i] = ipRPixel;
                            ipXPixels[i] = xo;
                            ipYPixels[i] = yo;
                        }
                    }
                }
            }
        }

        if (outputPolarCoordinates) {
            algebraicToPolar(
                    (int[]) ipX.getPixels(),
                    (int[]) ipY.getPixels(),
                    (float[]) r.getPixels(),
                    (float[]) phi.getPixels(),
                    relativeDistances ? maxDistance : 1.0);
        } else {
            algebraicToCartesian(
                    (int[]) ipX.getPixels(),
                    (int[]) ipY.getPixels(),
                    (float[]) r.getPixels(),
                    (float[]) phi.getPixels(),
                    relativeDistances ? maxDistance : 1.0);
        }
    }

    private void algebraicToPolar(
            final int[] ipXPixels,
            final int[] ipYPixels,
            final float[] ipRPixels,
            final float[] ipPhiPixels,
            final double max) {
        final int n = ipXPixels.length;
        for (int i = 0; i < n; ++i) {
            final double x = ipXPixels[i] / max;
            final double y = ipYPixels[i] / max;

            final double r = Math.sqrt(x * x + y * y);
            final double phi = Math.atan2(x / r, y / r);

            ipRPixels[i] = (float) r;
            ipPhiPixels[i] = (float) phi;
        }
    }

    private void algebraicToCartesian(
            final int[] ipXPixels,
            final int[] ipYPixels,
            final float[] ipCXPixels,
            final float[] ipCYhiPixels,
            final double max) {
        final int n = ipXPixels.length;
        for (int i = 0; i < n; ++i) {
            final double x = ipXPixels[i] / max;
            final double y = ipYPixels[i] / max;

            ipCXPixels[i] = (float) x;
            ipCYhiPixels[i] = (float) y;
        }
    }

}
