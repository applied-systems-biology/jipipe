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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link RankFilters}
 */
@JIPipeDocumentation(name = "Replace NaN by local median", description = "Filter that removes NaN values from floating point images by replacing them by the local median. " +
        "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nNoise", aliasName = "Remove NaNs...")
public class RemoveNaNFilter2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double radius = 1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RemoveNaNFilter2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RemoveNaNFilter2DAlgorithm(RemoveNaNFilter2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        RankFilters rankFilters = new RankFilters();
        ImageJUtils.forEachSlice(img, ip -> rankFilters.rank(ip, radius, RankFilters.REMOVE_NAN), progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Radius", description = "Radius of the filter kernel. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @JIPipeParameter("radius")
    public double getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public boolean setRadius(double radius) {
        if (radius <= 0) {
            return false;
        }
        this.radius = radius;

        return true;
    }
}