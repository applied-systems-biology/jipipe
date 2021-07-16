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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.contrast;

import ij.ImagePlus;
import ij.ImageStack;
import mpicbg.ij.clahe.Flat;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Applies CLAHE image enhancing
 */
@JIPipeDocumentation(name = "Enhance local contrast (CLAHE)", description = "Applies 'Contrast Limited Adaptive Histogram Equalization' (CLAHE) to enhance contrast. " +
        "Composite color images are converted into their luminance. If higher-dimensional data is provided, the results are generated for each 2D slice.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Contrast")


@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")


public class CLAHEContrastEnhancer extends JIPipeSimpleIteratingAlgorithm {

    private int blockRadius = 127;
    private int bins = 256;
    private float maxSlope = 3.0f;
    private boolean fastMode = false;

    /**
     * @param info the info
     */
    public CLAHEContrastEnhancer(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();

        if (inputData.getImage().isStack()) {
            ImageStack stack = new ImageStack(inputData.getImage().getWidth(), inputData.getImage().getHeight(), inputData.getImage().getProcessor().getColorModel());
            ImageJUtils.forEachIndexedSlice(inputData.getImage(), (imp, index) -> {
                ImagePlus slice = new ImagePlus("slice", imp.duplicate());
                clahe.run(slice, blockRadius, bins, maxSlope, null, true);
                stack.addSlice(slice.getProcessor());
            }, progressInfo);
            ImagePlus result = new ImagePlus("CLAHE", stack);
            result.setDimensions(inputData.getImage().getNChannels(), inputData.getImage().getNSlices(), inputData.getImage().getNFrames());
            result.copyScale(inputData.getImage());
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        } else {
            ImagePlus result = inputData.getDuplicateImage();
            clahe.run(result, blockRadius, bins, maxSlope, null, true);
            result.copyScale(inputData.getImage());
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }

    @JIPipeParameter("block-radius")
    @JIPipeDocumentation(name = "Blocks")
    public int getBlockRadius() {
        return blockRadius;
    }

    @JIPipeParameter("block-radius")
    public void setBlockRadius(int blockRadius) {
        this.blockRadius = blockRadius;

    }

    @JIPipeParameter("bins")
    @JIPipeDocumentation(name = "Bins")
    public int getBins() {
        return bins;
    }

    @JIPipeParameter("bins")
    public void setBins(int bins) {
        this.bins = bins;

    }

    @JIPipeParameter("max-slope")
    @JIPipeDocumentation(name = "Max slope")
    public float getMaxSlope() {
        return maxSlope;
    }

    @JIPipeParameter("max-slope")
    public void setMaxSlope(float maxSlope) {
        this.maxSlope = maxSlope;

    }

    @JIPipeParameter("fast-mode")
    @JIPipeDocumentation(name = "Fast mode")
    public boolean isFastMode() {
        return fastMode;
    }

    @JIPipeParameter("fast-mode")
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;

    }
}