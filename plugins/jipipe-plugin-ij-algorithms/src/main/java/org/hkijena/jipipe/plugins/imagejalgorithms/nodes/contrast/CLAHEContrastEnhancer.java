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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.contrast;

import ij.ImagePlus;
import ij.ImageStack;
import mpicbg.ij.clahe.Flat;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;


/**
 * Applies CLAHE image enhancing
 */
@SetJIPipeDocumentation(name = "Enhance local contrast (CLAHE)", description = "Applies 'Contrast Limited Adaptive Histogram Equalization' (CLAHE) to enhance contrast. " +
        "Composite color images are converted into their luminance. If higher-dimensional data is provided, the results are generated for each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Enhance Local Contrast (CLAHE)")
public class CLAHEContrastEnhancer extends JIPipeSimpleIteratingAlgorithm {

    private int blockRadius = 127;
    private int bins = 256;
    private float maxSlope = 3.0f;
    private boolean fastMode = false;

    /**
     * @param info the info
     */
    public CLAHEContrastEnhancer(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public CLAHEContrastEnhancer(CLAHEContrastEnhancer other) {
        super(other);
        this.blockRadius = other.blockRadius;
        this.bins = other.bins;
        this.maxSlope = other.maxSlope;
        this.fastMode = other.fastMode;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();

        if (inputData.getImage().hasImageStack()) {
            ImageStack stack = new ImageStack(inputData.getImage().getWidth(), inputData.getImage().getHeight(), inputData.getImage().getProcessor().getColorModel());
            ImageJIterationUtils.forEachIndexedSlice(inputData.getImage(), (imp, index) -> {
                ImagePlus slice = new ImagePlus("slice", imp.duplicate());
                clahe.run(slice, blockRadius, bins, maxSlope, null, true);
                stack.addSlice(slice.getProcessor());
            }, progressInfo);
            ImagePlus result = new ImagePlus("CLAHE", stack);
            result.setDimensions(inputData.getImage().getNChannels(), inputData.getImage().getNSlices(), inputData.getImage().getNFrames());
            result.copyScale(inputData.getImage());
            // Do calibration afterward
            ImageJUtils.calibrate(result, ImageJCalibrationMode.AutomaticImageJ, 0, 0);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        } else {
            ImagePlus result = inputData.getDuplicateImage();
            clahe.run(result, blockRadius, bins, maxSlope, null, true);
            result.copyScale(inputData.getImage());
            // Do calibration afterward
            ImageJUtils.calibrate(result, ImageJCalibrationMode.AutomaticImageJ, 0, 0);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }

    @JIPipeParameter("block-radius")
    @SetJIPipeDocumentation(name = "Blocks")
    public int getBlockRadius() {
        return blockRadius;
    }

    @JIPipeParameter("block-radius")
    public void setBlockRadius(int blockRadius) {
        this.blockRadius = blockRadius;

    }

    @JIPipeParameter("bins")
    @SetJIPipeDocumentation(name = "Bins")
    public int getBins() {
        return bins;
    }

    @JIPipeParameter("bins")
    public void setBins(int bins) {
        this.bins = bins;

    }

    @JIPipeParameter("max-slope")
    @SetJIPipeDocumentation(name = "Max slope")
    public float getMaxSlope() {
        return maxSlope;
    }

    @JIPipeParameter("max-slope")
    public void setMaxSlope(float maxSlope) {
        this.maxSlope = maxSlope;

    }

    @JIPipeParameter("fast-mode")
    @SetJIPipeDocumentation(name = "Fast mode")
    public boolean isFastMode() {
        return fastMode;
    }

    @JIPipeParameter("fast-mode")
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;

    }
}