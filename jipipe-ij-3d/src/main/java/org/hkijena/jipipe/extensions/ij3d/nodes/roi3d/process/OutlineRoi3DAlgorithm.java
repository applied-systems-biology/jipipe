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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DOutline;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Outline 3D ROI", description = "Converts the ROI into bounding boxes, convex hulls, etc.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData data = (ROI3DListData) dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo).duplicate(progressInfo);
        data.outline(outline, ignoreErrors, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Outline method", description = "The outlining method that should be applied.")
    @JIPipeParameter("outline")
    public ROI3DOutline getOutline() {
        return outline;
    }

    @JIPipeParameter("outline")
    public void setOutline(ROI3DOutline outline) {
        this.outline = outline;
    }

    @JIPipeDocumentation(name = "Ignore errors", description = "If enabled, ignore any errors that occur. Affected objects will be deleted.")
    @JIPipeParameter("ignore-errors")
    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    @JIPipeParameter("ignore-errors")
    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }
}
