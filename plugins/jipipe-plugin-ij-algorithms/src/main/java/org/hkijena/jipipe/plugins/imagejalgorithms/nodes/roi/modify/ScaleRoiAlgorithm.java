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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.modify;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Scale 2D ROI (old)", description = "Scales all ROI in the ROI list. If you want to have more flexibility, use one of the 'Change ROI properties' nodes. A newer algorithm that allows to do scaling/rotating/translation in one is available.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
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
        ROI2DListData data = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);
        data = data.scale(scaleX, scaleY, centerScale);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Scale X", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI.")
    @JIPipeParameter("scale-x")
    public double getScaleX() {
        return scaleX;
    }

    @JIPipeParameter("scale-x")
    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    @SetJIPipeDocumentation(name = "Scale Y", description = "Scales the ROI. Please note that the scale will not be saved inside the ROI.")
    @JIPipeParameter("scale-y")
    public double getScaleY() {
        return scaleY;
    }

    @JIPipeParameter("scale-y")
    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    @SetJIPipeDocumentation(name = "Center scale", description = "If true, each ROI is scaled relative to its center.")
    @JIPipeParameter("center-scale")
    public boolean getCenterScale() {
        return centerScale;
    }

    @JIPipeParameter("center-scale")
    public void setCenterScale(boolean centerScale) {
        this.centerScale = centerScale;
    }
}
