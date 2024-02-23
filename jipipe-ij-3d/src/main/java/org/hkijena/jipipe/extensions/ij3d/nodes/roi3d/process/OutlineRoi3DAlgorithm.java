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

package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.process;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DOutline;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Outline 3D ROI", description = "Converts the ROI into bounding boxes, convex hulls, etc.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", create = true)
public class OutlineRoi3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ROI3DOutline outline = ROI3DOutline.BoundingBox;
    private boolean ignoreErrors;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public OutlineRoi3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public OutlineRoi3DAlgorithm(OutlineRoi3DAlgorithm other) {
        super(other);
        this.outline = other.outline;
        this.ignoreErrors = other.ignoreErrors;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData data = (ROI3DListData) iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo).duplicate(progressInfo);
        data.outline(outline, ignoreErrors, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Outline method", description = "The outlining method that should be applied.")
    @JIPipeParameter("outline")
    public ROI3DOutline getOutline() {
        return outline;
    }

    @JIPipeParameter("outline")
    public void setOutline(ROI3DOutline outline) {
        this.outline = outline;
    }

    @SetJIPipeDocumentation(name = "Ignore errors", description = "If enabled, ignore any errors that occur. Affected objects will be deleted.")
    @JIPipeParameter("ignore-errors")
    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    @JIPipeParameter("ignore-errors")
    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }
}
