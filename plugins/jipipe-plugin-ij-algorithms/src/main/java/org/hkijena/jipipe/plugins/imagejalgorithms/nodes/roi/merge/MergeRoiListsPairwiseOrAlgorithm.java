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

import ij.gui.Roi;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;

import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Merge 2D ROI lists (pairwise OR)", description = "Merges each individual ROI in Target with each individual ROI in Source, generating all pairwise combinations of all ROI.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Target", create = true, description = "Where the ROI are added")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Source", create = true, description = "The ROI to be added")
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class MergeRoiListsPairwiseOrAlgorithm extends JIPipeMergingAlgorithm {

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public MergeRoiListsPairwiseOrAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public MergeRoiListsPairwiseOrAlgorithm(MergeRoiListsPairwiseOrAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<ROI2DListData> targetRoiLists = iterationStep.getInputData("Target", ROI2DListData.class, progressInfo);
        List<ROI2DListData> sourceRoiLists = iterationStep.getInputData("Source", ROI2DListData.class, progressInfo);

        // Merge all into one list
        ROI2DListData targetRois = new ROI2DListData();
        ROI2DListData sourceRois = new ROI2DListData();

        for (ROI2DListData targetRoiList : targetRoiLists) {
            targetRois.addAll(targetRoiList);
        }
        for (ROI2DListData sourceRoiList : sourceRoiLists) {
            sourceRois.addAll(sourceRoiList);
        }

        // pairwise iteration
        ROI2DListData result = new ROI2DListData();
        for (Roi roi1 : targetRois) {
            for (Roi roi2 : sourceRois) {
                if (roi1 != roi2) {
                    ROI2DListData tmp = new ROI2DListData();
                    tmp.add(roi1);
                    tmp.add(roi2);
                    tmp.logicalOr();
                    result.addAll(tmp);
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }
}
