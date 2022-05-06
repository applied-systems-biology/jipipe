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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Merge ROI lists", description = "Merges multiple ROI lists by using data annotations. " +
        "By default, ROIs with equivalent annotations are put into the same group and merged into one ROI list for each group. " +
        "Use the parameters to control how groups are created. To merge all incoming ROI lists into just one list, set the matching strategy to 'Custom' and leave the list of " +
        "annotation columns empty.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class MergeRoiListsAlgorithm extends JIPipeMergingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MergeRoiListsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ROIListData.class)
                .addOutputSlot("Output", "", ROIListData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MergeRoiListsAlgorithm(MergeRoiListsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData result = new ROIListData();
        for (ROIListData rois : dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo)) {
            result.mergeWith(rois);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }
}
