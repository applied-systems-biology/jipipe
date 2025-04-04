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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.blur;

import ij.ImagePlus;
import ij.plugin.filter.Convolver;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.util.Arrays;


/**
 * Wrapper around {@link RankFilters}
 */
@SetJIPipeDocumentation(name = "Box filter 2D", description = "Applies a box (local average) filter. " +
        "Unlike local mean, this filter uses a filter kernel where all pixels have the same weight. " +
        "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Filter", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Smooth (any radius)")
public class BoxFilter2Dv2Algorithm extends JIPipeSimpleIteratingAlgorithm {

    private double radius = 1;

    public BoxFilter2Dv2Algorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public BoxFilter2Dv2Algorithm(BoxFilter2Dv2Algorithm other) {
        super(other);
        this.radius = other.radius;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        Convolver convolver = new Convolver();
        final int kernelSize = (int) (radius * 2 + 1);
        float[] kernel = new float[kernelSize * kernelSize];
        Arrays.fill(kernel, 1);

        convolver.setNormalize(true);
        ImageJIterationUtils.forEachSlice(img, imp -> {
            ImageJUtils.convolveSlice(convolver, kernelSize, kernelSize, kernel, imp);
        }, progressInfo);

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
}
