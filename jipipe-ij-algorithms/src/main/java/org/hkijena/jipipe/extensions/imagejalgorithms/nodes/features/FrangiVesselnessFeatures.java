/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.features;

import fiji.features.Frangi_;
import fiji.features.MultiTaskProgress;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import mpicbg.imglib.type.numeric.real.FloatType;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;


/**
 * Applies CLAHE image enhancing
 */
@SetJIPipeDocumentation(name = "Frangi vesselness", description = "Applies the vesselness filter developed by Frangi et al.")
@DefineJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFilters", aliasName = "Frangi Vesselness")
public class FrangiVesselnessFeatures extends JIPipeSimpleIteratingAlgorithm {

    private int numScales = 1;
    private double minimumScale = 3.0;
    private double maximumScale = 3.0;
    private boolean invert = false;
    private SlicingMode slicingMode = SlicingMode.Unchanged;

    /**
     * @param info the info
     */
    public FrangiVesselnessFeatures(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public FrangiVesselnessFeatures(FrangiVesselnessFeatures other) {
        super(other);
        this.numScales = other.numScales;
        this.minimumScale = other.minimumScale;
        this.maximumScale = other.maximumScale;
        this.invert = other.invert;
        this.slicingMode = other.slicingMode;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public int getParallelizationBatchSize() {
        // Frangi_ does its own parallelization
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusGreyscale32FData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
        ImagePlus img = inputData.getImage();

        if (invert) {
            img = ImageJUtils.duplicate(img);
            ImageJUtils.forEachSlice(img, ImageProcessor::invert, progressInfo);
        }

        Frangi_<FloatType> frangi = new Frangi_<>();
        MultiTaskProgress multiTaskProgress = new MultiTaskProgress() {
            @Override
            public void updateProgress(double proportionDone, int taskIndex) {
                progressInfo.log("Frangi vesselness " + (int) (proportionDone * 100) + "%");
            }

            @Override
            public void done() {

            }
        };
        ImagePlus result;

        if (slicingMode == SlicingMode.Unchanged) {
            result = frangi.process(img,
                    numScales,
                    minimumScale,
                    maximumScale,
                    false,
                    false,
                    false,
                    multiTaskProgress);
        } else if (slicingMode == SlicingMode.ApplyPer2DSlice) {
            ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
            ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
                ImagePlus slice = new ImagePlus("slice", imp);
                ImagePlus processedSlice = frangi.process(slice,
                        numScales,
                        minimumScale,
                        maximumScale,
                        false,
                        false,
                        false,
                        multiTaskProgress);
                stack.addSlice("slice" + index, processedSlice.getProcessor());
            }, progressInfo);
            result = new ImagePlus("Vesselness", stack);
            result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
            result.copyScale(img);
        } else {
            throw new UnsupportedOperationException("Not implemented: " + slicingMode);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @JIPipeParameter("num-scales")
    @SetJIPipeDocumentation(name = "Scales", description = "How many intermediate steps between minimum and maximum scales should be applied.")
    public int getNumScales() {
        return numScales;
    }

    @JIPipeParameter("num-scales")
    public void setNumScales(int numScales) {
        this.numScales = numScales;

    }

    @JIPipeParameter("min-scale")
    @SetJIPipeDocumentation(name = "Minimum scale", description = "The minimum scale that is applied")
    public double getMinimumScale() {
        return minimumScale;
    }

    @JIPipeParameter("min-scale")
    public void setMinimumScale(double minimumScale) {
        this.minimumScale = minimumScale;

    }

    @JIPipeParameter("max-scale")
    @SetJIPipeDocumentation(name = "Maximum scale", description = "The maximum scale that is applied")
    public double getMaximumScale() {
        return maximumScale;
    }

    @JIPipeParameter("max-scale")
    public void setMaximumScale(double maximumScale) {
        this.maximumScale = maximumScale;

    }

    @SetJIPipeDocumentation(name = "Invert colors", description = "Invert colors before applying the filter. This is useful if you look for bright structures within a dark background.")
    @JIPipeParameter("invert")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @SetJIPipeDocumentation(name = "Apply per slice", description = "Applies the vesselness filter for each 2D slice instead for the whole multi-dimensional image.")
    @JIPipeParameter("per-slice")
    public SlicingMode getSlicingMode() {
        return slicingMode;
    }

    @JIPipeParameter("per-slice")
    public void setSlicingMode(SlicingMode slicingMode) {
        this.slicingMode = slicingMode;
    }

    /**
     * Available ways how to handle higher-dimensional images
     */
    public enum SlicingMode {
        Unchanged,
        ApplyPer2DSlice
    }
}