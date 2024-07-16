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

import ij.gui.Roi;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiOutline;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Flatten 2D ROI", description = "Removes the Z/C/T coordinates of ROI. Equivalent to setting Z/C/T to zero.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = ROIListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Output", create = true)
public class FlattenRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean flattenZ = true;
    private boolean flattenC = true;
    private boolean flattenT = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FlattenRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FlattenRoiAlgorithm(FlattenRoiAlgorithm other) {
        super(other);
        this.flattenZ = other.flattenZ;
        this.flattenC = other.flattenC;
        this.flattenT = other.flattenT;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo).duplicate(progressInfo);
        for (Roi roi : data) {
            int z = roi.getZPosition();
            int c = roi.getCPosition();
            int t = roi.getTPosition();
            if(flattenZ) {
                z = 0;
            }
            if(flattenC) {
                c = 0;
            }
            if(flattenT) {
                t = 0;
            }
            roi.setPosition(c, z, t);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Flatten Z", description = "Set the Z location to zero")
    @JIPipeParameter("flatten-z")
    public boolean isFlattenZ() {
        return flattenZ;
    }

    @JIPipeParameter("flatten-z")
    public void setFlattenZ(boolean flattenZ) {
        this.flattenZ = flattenZ;
    }

    @SetJIPipeDocumentation(name = "Flatten C", description = "Set the channel location to zero")
    @JIPipeParameter("flatten-c")
    public boolean isFlattenC() {
        return flattenC;
    }

    @JIPipeParameter("flatten-c")
    public void setFlattenC(boolean flattenC) {
        this.flattenC = flattenC;
    }

    @SetJIPipeDocumentation(name = "Flatten T", description = "Set the frame location to zero")
    @JIPipeParameter("flatten-t")
    public boolean isFlattenT() {
        return flattenT;
    }

    @JIPipeParameter("flatten-t")
    public void setFlattenT(boolean flattenT) {
        this.flattenT = flattenT;
    }
}
