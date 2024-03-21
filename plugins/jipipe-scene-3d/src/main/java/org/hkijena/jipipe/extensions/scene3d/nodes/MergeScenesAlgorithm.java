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

package org.hkijena.jipipe.extensions.scene3d.nodes;


import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;

@SetJIPipeDocumentation(name = "Merge 3D scenes", description = "Merges the input scenes into one")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "3D Scenes")
@AddJIPipeInputSlot(value = Scene3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", create = true)
public class MergeScenesAlgorithm extends JIPipeMergingAlgorithm {

    public MergeScenesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeScenesAlgorithm(MergeScenesAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Scene3DData outputData = new Scene3DData();
        for (Scene3DData nodes : iterationStep.getInputData(getFirstInputSlot(), Scene3DData.class, progressInfo)) {
            outputData.addAll(nodes);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
