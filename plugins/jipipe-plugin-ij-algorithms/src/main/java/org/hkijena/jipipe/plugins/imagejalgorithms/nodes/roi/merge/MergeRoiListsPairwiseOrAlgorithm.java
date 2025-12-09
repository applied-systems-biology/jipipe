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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;

import java.util.List;


@SetJIPipeDocumentation(name = "Merge 2D ROI lists (pairwise OR)", description = "Merges each individual ROI in Target with each individual ROI in Source, generating all pairwise combinations of all ROI.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "Target", create = true, description = "Where the ROI are added")
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "Source", create = true, description = "The ROI to be added")
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "Output", create = true)
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
        List<Roi2dListData> targetRoiLists = iterationStep.getInputData("Target", Roi2dListData.class, progressInfo);
        List<Roi2dListData> sourceRoiLists = iterationStep.getInputData("Source", Roi2dListData.class, progressInfo);

        // Merge all into one list
        Roi2dListData targetRois = new Roi2dListData();
        Roi2dListData sourceRois = new Roi2dListData();

        for (Roi2dListData targetRoiList : targetRoiLists) {
            targetRois.addAll(targetRoiList);
        }
        for (Roi2dListData sourceRoiList : sourceRoiLists) {
            sourceRois.addAll(sourceRoiList);
        }

        // pairwise iteration
        Roi2dListData result = new Roi2dListData();
        for (Roi roi1 : targetRois) {
            for (Roi roi2 : sourceRois) {
                if (progressInfo.isCancelled()) {
                    return;
                }
                if (roi1 != roi2) {
                    Roi2dListData tmp = new Roi2dListData();
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
