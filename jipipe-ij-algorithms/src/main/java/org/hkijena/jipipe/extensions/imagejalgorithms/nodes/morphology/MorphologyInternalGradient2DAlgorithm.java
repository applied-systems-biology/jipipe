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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.morphology;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.RankFilters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Internal gradient 2D", description = "Applies an internal gradient filter that subtracts the local minimum (erosion) from the input image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class MorphologyInternalGradient2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double radius = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MorphologyInternalGradient2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MorphologyInternalGradient2DAlgorithm(MorphologyInternalGradient2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    private void applyInternalGradient(ImagePlus img) {
        // Erode the original image
        ImagePlus eroded = ImageJUtils.duplicate(img);
        RankFilters erosionFilter = new RankFilters();
        erosionFilter.rank(eroded.getProcessor(), radius, RankFilters.MIN); //TODO: Set element to octagon

        // Apply image calculator
        ImageCalculator calculator = new ImageCalculator();
        calculator.run("Subtract", img, eroded);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            progressInfo.log("Slice " + index + "/" + img.getStackSize());
            ImagePlus slice = new ImagePlus("slice", imp.duplicate());
            applyInternalGradient(slice);
            stack.addSlice("slice" + index, slice.getProcessor());
        }, progressInfo);
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "Radius", description = "Radius of the local minimum / erosion filter")
    @JIPipeParameter("radius")
    public double getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(double radius) {
        this.radius = radius;
    }
}
