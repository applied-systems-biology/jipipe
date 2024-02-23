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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.blur;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Filters3D;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;


/**
 * Wrapper around {@link GaussianBlur}
 */
@SetJIPipeDocumentation(name = "Box filter 3D", description = "Applies convolution with a local mean function in 3D space for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 3D slice.")
@ConfigureJIPipeNode(menuPath = "Blur", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Smooth (3D)")
public class BoxFilter3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private float radiusX = 2;
    private float radiusY = -1;
    private float radiusZ = -1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public BoxFilter3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public BoxFilter3DAlgorithm(BoxFilter3DAlgorithm other) {
        super(other);
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageStack filtered = Filters3D.filter(img.getStack(), Filters3D.MEAN, radiusX, radiusY <= 0 ? radiusX : radiusY, radiusZ <= 0 ? radiusX : radiusZ);
        ImagePlus result = new ImagePlus("Output", filtered);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Radius (X)", description = "Filter radius (pixels) in X direction. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @JIPipeParameter("radius-x")
    public float getRadiusX() {
        return radiusX;
    }

    @JIPipeParameter("radius-x")
    public void setRadiusX(float radiusX) {
        this.radiusX = radiusX;

    }

    @SetJIPipeDocumentation(name = "Radius (Y)", description = "Filter radius (pixels) in Y direction." +
            " If zero or less, radius in X direction is automatically used instead. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @JIPipeParameter("radius-y")
    public float getRadiusY() {
        return radiusY;
    }

    @JIPipeParameter("radius-y")
    public void setRadiusY(float radiusY) {
        this.radiusY = radiusY;

    }

    @SetJIPipeDocumentation(name = "Radius (Z)", description = "Filter radius (pixels) in Z direction." +
            " If zero or less, radius in X direction is automatically used instead. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @JIPipeParameter("radius-z")
    public float getRadiusZ() {
        return radiusZ;
    }

    @JIPipeParameter("radius-z")
    public void setRadiusZ(float radiusZ) {
        this.radiusZ = radiusZ;

    }
}
