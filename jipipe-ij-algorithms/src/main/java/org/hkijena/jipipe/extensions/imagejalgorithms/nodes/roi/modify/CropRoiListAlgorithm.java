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
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Crop ROI list", description = "Moves all ROI in the list so that their bounding rectangle is located at x=0 and y=0 (default). Also allows to crop the channel, time, and Z locations.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class CropRoiListAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean cropXY = true;
    private boolean cropZ = false;
    private boolean cropT = false;
    private boolean cropC = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public CropRoiListAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CropRoiListAlgorithm(CropRoiListAlgorithm other) {
        super(other);
        this.cropXY = other.cropXY;
        this.cropZ = other.cropZ;
        this.cropT = other.cropT;
        this.cropC = other.cropC;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo).duplicate(progressInfo);
        data.crop(cropXY, cropZ, cropC, cropT);
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Crop X/Y", description = "If enabled, crop the ROI list in the X and Y axis")
    @JIPipeParameter("crop-xy")
    public boolean isCropXY() {
        return cropXY;
    }

    @JIPipeParameter("crop-xy")
    public void setCropXY(boolean cropXY) {
        this.cropXY = cropXY;
    }

    @JIPipeDocumentation(name = "Crop Z", description = "If enabled, crop the ROI list in the Z axis (locations with a value of zero are skipped)")
    @JIPipeParameter("crop-z")
    public boolean isCropZ() {
        return cropZ;
    }

    @JIPipeParameter("crop-z")
    public void setCropZ(boolean cropZ) {
        this.cropZ = cropZ;
    }

    @JIPipeDocumentation(name = "Crop T", description = "If enabled, crop the ROI list in the T axis (locations with a value of zero are skipped)")
    @JIPipeParameter("crop-t")
    public boolean isCropT() {
        return cropT;
    }

    @JIPipeParameter("crop-t")
    public void setCropT(boolean cropT) {
        this.cropT = cropT;
    }

    @JIPipeDocumentation(name = "Crop C", description = "If enabled, crop the ROI list in the channel axis (locations with a value of zero are skipped)")
    @JIPipeParameter("crop-c")
    public boolean isCropC() {
        return cropC;
    }

    @JIPipeParameter("crop-c")
    public void setCropC(boolean cropC) {
        this.cropC = cropC;
    }
}
