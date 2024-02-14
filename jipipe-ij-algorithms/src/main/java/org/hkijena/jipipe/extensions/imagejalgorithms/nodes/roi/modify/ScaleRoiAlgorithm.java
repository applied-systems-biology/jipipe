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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.modify;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Scale ROI 2D", description = "Scales all ROI in the ROI list. If you want to have more flexibility, use one of the 'Change ROI properties' nodes.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ScaleRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private boolean centerScale = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ScaleRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }


    public ScaleRoiAlgorithm(ScaleRoiAlgorithm other) {
        super(other);
        this.scaleX = other.scaleX;
        this.scaleY = other.scaleY;
        this.centerScale = other.centerScale;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData data = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        data = data.scale(scaleX, scaleY, centerScale);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Scale X", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI.")
    @JIPipeParameter("scale-x")
    public double getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    @JIPipeDocumentation(name = "Scale Y", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI.")
    @JIPipeParameter("scale-y")
    public double getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    @JIPipeDocumentation(name = "Center scale", description = "If true, each ROI is scaled relative to its center.")
    @JIPipeParameter("center-scale")
    public boolean getCenterScale() {
        return centerScale;
    }

    @JIPipeParameter("center-scale")
    public void setCenterScale(boolean centerScale) {
        this.centerScale = centerScale;
    }
}
