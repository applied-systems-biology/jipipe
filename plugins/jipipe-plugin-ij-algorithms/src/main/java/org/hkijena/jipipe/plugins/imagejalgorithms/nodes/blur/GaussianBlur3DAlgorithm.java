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
import ij.plugin.GaussianBlur3D;
import ij.plugin.filter.GaussianBlur;
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


/**
 * Wrapper around {@link GaussianBlur}
 */
@SetJIPipeDocumentation(name = "Gaussian blur 3D", description = "Applies convolution with a Gaussian function in 3D space for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 3D slice.")
@ConfigureJIPipeNode(menuPath = "Blur", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFilters", aliasName = "Gaussian Blur 3D...")
public class GaussianBlur3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double sigmaX = 1;
    private double sigmaY = -1;
    private double sigmaZ = -1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GaussianBlur3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GaussianBlur3DAlgorithm(GaussianBlur3DAlgorithm other) {
        super(other);
        this.sigmaX = other.sigmaX;
        this.sigmaY = other.sigmaY;
        this.sigmaZ = other.sigmaZ;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        GaussianBlur3D.blur(img, sigmaX, sigmaY <= 0 ? sigmaX : sigmaY, sigmaZ <= 0 ? sigmaX : sigmaZ);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }


    @SetJIPipeDocumentation(name = "Sigma (X)", description = "Standard deviation of the Gaussian (pixels) in X direction. ")
    @JIPipeParameter("sigma-x")
    public double getSigmaX() {
        return sigmaX;
    }

    @JIPipeParameter("sigma-x")
    public void setSigmaX(double sigmaX) {
        this.sigmaX = sigmaX;

    }

    @SetJIPipeDocumentation(name = "Sigma (Y)", description = "Standard deviation of the Gaussian (pixels) in Y direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @JIPipeParameter("sigma-y")
    public double getSigmaY() {
        return sigmaY;
    }

    @JIPipeParameter("sigma-y")
    public void setSigmaY(double sigmaY) {
        this.sigmaY = sigmaY;

    }

    @SetJIPipeDocumentation(name = "Sigma (Z)", description = "Standard deviation of the Gaussian (pixels) in Z direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @JIPipeParameter("sigma-z")
    public double getSigmaZ() {
        return sigmaZ;
    }

    @JIPipeParameter("sigma-z")
    public void setSigmaZ(double sigmaZ) {
        this.sigmaZ = sigmaZ;

    }
}
