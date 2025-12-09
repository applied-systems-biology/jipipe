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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.merge;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;


@SetJIPipeDocumentation(name = "Merge 2D ROI lists (ordered)", description = "Merges multiple ROI lists. The ROI from 'Source' are added to the end of the 'Target' list. Compared to 'Merge ROI lists', this node allows to control the order of the operation.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "Target", create = true, description = "Where the ROI are added")
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "Source", create = true, description = "The ROI to be added")
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "Output", create = true)
public class MergeRoiListsOrderedAlgorithm extends JIPipeMergingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MergeRoiListsOrderedAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MergeRoiListsOrderedAlgorithm(MergeRoiListsOrderedAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Roi2dListData result = new Roi2dListData();
        for (Roi2dListData rois : iterationStep.getInputData("Target", Roi2dListData.class, progressInfo)) {
            result.mergeWith(rois);
        }
        for (Roi2dListData rois : iterationStep.getInputData("Source", Roi2dListData.class, progressInfo)) {
            result.mergeWith(rois);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }
}
