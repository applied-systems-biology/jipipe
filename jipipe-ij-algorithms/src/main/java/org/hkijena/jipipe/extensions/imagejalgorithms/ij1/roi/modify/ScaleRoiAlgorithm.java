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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.modify;

import ij.gui.Roi;
import ij.plugin.RoiScaler;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Scale ROI", description = "Scales all ROI in the ROI list. If you want to have more flexibility, use one of the 'Change ROI properties' nodes.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
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
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ROIListData.class)
                .addOutputSlot("Output", "", ROIListData.class, null)
                .seal()
                .build());
    }


    public ScaleRoiAlgorithm(ScaleRoiAlgorithm other) {
        super(other);
        this.scaleX = other.scaleX;
        this.scaleY = other.scaleY;
        this.centerScale = other.centerScale;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo).duplicate(progressInfo);
        for (int i = 0; i < data.size(); i++) {
            Roi roi = data.get(i);
            if(scaleX != 1.0 || scaleY != 1.0) {
                roi = RoiScaler.scale(roi, scaleX, scaleY, centerScale);
                data.set(i, roi);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
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
