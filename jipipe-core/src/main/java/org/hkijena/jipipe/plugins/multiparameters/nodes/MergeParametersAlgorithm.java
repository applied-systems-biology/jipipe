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

package org.hkijena.jipipe.plugins.multiparameters.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;

@SetJIPipeDocumentation(name = "Merge parameters", description = "Merges multiple parameter sets. To always multiply all incoming parameters, set the iteration step grouping method to 'Multiply'. " +
        "Parameters with the same unique key are overwritten according to the input slot order.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Parameters")
@AddJIPipeInputSlot(value = ParametersData.class, name = "Input 1", create = true)
@AddJIPipeInputSlot(value = ParametersData.class, name = "Input 2", create = true)
@AddJIPipeOutputSlot(value = ParametersData.class, name = "Output", create = true)
public class MergeParametersAlgorithm extends JIPipeIteratingAlgorithm {
    public MergeParametersAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(ParametersData.class)
                .addFromAnnotations(MergeParametersAlgorithm.class)
                .sealOutput()
                .build());
    }

    public MergeParametersAlgorithm(MergeParametersAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ParametersData outputData = new ParametersData();
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            ParametersData inputData = iterationStep.getInputData(inputSlot, ParametersData.class, progressInfo);
            outputData.getParameterData().putAll(inputData.getParameterData());
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }
}
