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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.noise;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
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


/**
 * Wrapper around {@link RankFilters}
 */
@SetJIPipeDocumentation(name = "Remove outliers 2D", description = "Filter that replaces pixel values by the median if they deviate too much from it. " +
        "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Math", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nNoise", aliasName = "Remove Outliers...")
public class RemoveOutliersFilter2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double radius = 1;
    private float threshold = 50;
    private Mode mode = Mode.RemoveSmallerThanMedian;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RemoveOutliersFilter2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RemoveOutliersFilter2DAlgorithm(RemoveOutliersFilter2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
        this.mode = other.mode;
        this.threshold = other.threshold;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        RankFilters rankFilters = new RankFilters();
        ImageJIterationUtils.forEachSlice(img, ip -> rankFilters.rank(ip,
                radius,
                RankFilters.OUTLIERS,
                mode == Mode.RemoveLargerThanMedian ? RankFilters.BRIGHT_OUTLIERS : RankFilters.DARK_OUTLIERS,
                threshold), progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Radius", description = "Radius of the filter kernel. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
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

    @SetJIPipeDocumentation(name = "Mode", description = "Determines whether to modify pixels smaller or greater than the local median.")
    @JIPipeParameter("mode")
    public Mode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;

    }

    @SetJIPipeDocumentation(name = "Threshold", description = "Determines by how much a pixel has to deviate from the local median to be replaced by it.")
    @JIPipeParameter("threshold")
    public float getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public boolean setThreshold(float threshold) {
        if (threshold < 0) {
            return false;
        }
        this.threshold = threshold;

        return true;
    }

    /**
     * The two different modes of this filter
     */
    public enum Mode {
        RemoveSmallerThanMedian,
        RemoveLargerThanMedian;


        @Override
        public String toString() {
            switch (this) {
                case RemoveLargerThanMedian:
                    return "Remove smaller than median";
                case RemoveSmallerThanMedian:
                    return "Remove larger than median";
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
