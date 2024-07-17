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
@SetJIPipeDocumentation(name = "Crop 2D ROI list", description = "Moves all ROI in the list so that their bounding rectangle is located at x=0 and y=0 (default). Also allows to crop the channel, time, and Z locations.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData data = (ROI2DListData) iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo).duplicate(progressInfo);
        data.crop(cropXY, cropZ, cropC, cropT);
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Crop X/Y", description = "If enabled, crop the ROI list in the X and Y axis")
    @JIPipeParameter("crop-xy")
    public boolean isCropXY() {
        return cropXY;
    }

    @JIPipeParameter("crop-xy")
    public void setCropXY(boolean cropXY) {
        this.cropXY = cropXY;
    }

    @SetJIPipeDocumentation(name = "Crop Z", description = "If enabled, crop the ROI list in the Z axis (locations with a value of zero are skipped)")
    @JIPipeParameter("crop-z")
    public boolean isCropZ() {
        return cropZ;
    }

    @JIPipeParameter("crop-z")
    public void setCropZ(boolean cropZ) {
        this.cropZ = cropZ;
    }

    @SetJIPipeDocumentation(name = "Crop T", description = "If enabled, crop the ROI list in the T axis (locations with a value of zero are skipped)")
    @JIPipeParameter("crop-t")
    public boolean isCropT() {
        return cropT;
    }

    @JIPipeParameter("crop-t")
    public void setCropT(boolean cropT) {
        this.cropT = cropT;
    }

    @SetJIPipeDocumentation(name = "Crop C", description = "If enabled, crop the ROI list in the channel axis (locations with a value of zero are skipped)")
    @JIPipeParameter("crop-c")
    public boolean isCropC() {
        return cropC;
    }

    @JIPipeParameter("crop-c")
    public void setCropC(boolean cropC) {
        this.cropC = cropC;
    }
}
