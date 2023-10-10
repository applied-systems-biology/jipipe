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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.merge;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Combine ROI lists", description = "Merges multiple ROI lists. The ROI from 'Source' are added to the end of the 'Target' list. Compared to 'Merge ROI lists', this node allows to control the order of the operation.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Target", autoCreate = true, description = "Where the ROI are added")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Source", autoCreate = true, description = "The ROI to be added")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class AddRoiListsAlgorithm extends JIPipeMergingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public AddRoiListsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public AddRoiListsAlgorithm(AddRoiListsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData result = new ROIListData();
        for (ROIListData rois : dataBatch.getInputData("Target", ROIListData.class, progressInfo)) {
            result.mergeWith(rois);
        }
        for (ROIListData rois : dataBatch.getInputData("Source", ROIListData.class, progressInfo)) {
            result.mergeWith(rois);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }
}
