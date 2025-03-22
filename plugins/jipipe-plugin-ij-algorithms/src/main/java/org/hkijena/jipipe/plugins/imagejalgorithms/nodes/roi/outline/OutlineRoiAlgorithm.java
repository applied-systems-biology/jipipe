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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.outline;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiOutline;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Outline 2D ROI (Hull)", description = "Applies one of the following operations to all ROI in the list: " +
        "<ul>" +
        "<li>Convert to closed polygon</li>" +
        "<li>Convert to polygon</li>" +
        "<li>Calculate convex hull</li>" +
        "<li>Convert to bounding rectangle</li>" +
        "<li>Convert to minimum bounding rectangle (rotated rectangle)</li>" +
        "<li>Convert to oriented line</li>" +
        "<li>Convert to fitted circle</li>" +
        "</ul>")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Outline")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class OutlineRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private RoiOutline outline = RoiOutline.ClosedPolygon;
    private boolean ignoreErrors = false;

    public OutlineRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OutlineRoiAlgorithm(OutlineRoiAlgorithm other) {
        super(other);
        this.outline = other.outline;
        this.ignoreErrors = other.ignoreErrors;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData data = (ROI2DListData) iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo).duplicate(progressInfo);
        data.outline(outline, ignoreErrors);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Outline method", description = "The outlining method that should be applied.")
    @JIPipeParameter("outline")
    public RoiOutline getOutline() {
        return outline;
    }

    @JIPipeParameter("outline")
    public void setOutline(RoiOutline outline) {
        this.outline = outline;
    }

    @SetJIPipeDocumentation(name = "Ignore errors", description = "If enabled, skip ROI that cannot be outlined")
    @JIPipeParameter("ignore-errors")
    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    @JIPipeParameter("ignore-errors")
    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }
}
