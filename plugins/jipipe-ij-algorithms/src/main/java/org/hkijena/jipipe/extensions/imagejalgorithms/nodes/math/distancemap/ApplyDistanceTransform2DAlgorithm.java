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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.math.distancemap;

import ij.ImagePlus;
import ij.plugin.filter.EDM;
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
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link EDM}
 */
@SetJIPipeDocumentation(name = "Euclidean distance transform 2D", description = "Applies a euclidean distance transform on binary images." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Math\nDistance map", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nBinary", aliasName = "Distance Map")
public class ApplyDistanceTransform2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int backgroundValue = 0;

    private boolean edgesAreBackground = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ApplyDistanceTransform2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ApplyDistanceTransform2DAlgorithm(ApplyDistanceTransform2DAlgorithm other) {
        super(other);
        this.backgroundValue = other.backgroundValue;
        this.edgesAreBackground = other.edgesAreBackground;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Background value", description = "Pixels in the input with this value are interpreted as background.")
    @JIPipeParameter("background-value")
    public int getBackgroundValue() {
        return backgroundValue;
    }

    @JIPipeParameter("background-value")
    public void setBackgroundValue(int backgroundValue) {
        this.backgroundValue = backgroundValue;
    }

    @SetJIPipeDocumentation(name = "Edges are background", description = "Whether out-of-image pixels are considered background")
    @JIPipeParameter("edges-are-background")
    public boolean isEdgesAreBackground() {
        return edgesAreBackground;
    }

    @JIPipeParameter("edges-are-background")
    public void setEdgesAreBackground(boolean edgesAreBackground) {
        this.edgesAreBackground = edgesAreBackground;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo);
        ImagePlus img = inputData.getImage();
        EDM edm = new EDM();
        ImagePlus result = ImageJUtils.generateForEachIndexedZCTSlice(img, (ip, index) -> edm.makeFloatEDM(ip, backgroundValue, edgesAreBackground), progressInfo);
        result.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(result), progressInfo);
    }
}
