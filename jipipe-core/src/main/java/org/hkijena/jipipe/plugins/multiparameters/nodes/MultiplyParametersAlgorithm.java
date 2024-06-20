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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;

@SetJIPipeDocumentation(name = "Multiply parameters", description = "Provide multiple parameter sets via the inputs to calculate all parameter combinations.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Parameters")
@AddJIPipeInputSlot(value = ParametersData.class, name = "Input 1", create = true)
@AddJIPipeInputSlot(value = ParametersData.class, name = "Input 2", create = true)
@AddJIPipeOutputSlot(value = ParametersData.class, name = "Output", create = true)
public class MultiplyParametersAlgorithm extends JIPipeAlgorithm {
    public MultiplyParametersAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addFromAnnotations(MultiplyParametersAlgorithm.class)
                .restrictInputTo(ParametersData.class)
                .sealOutput()
                .build());
    }

    public MultiplyParametersAlgorithm(MultiplyParametersAlgorithm other) {
        super(other);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        MergeParametersAlgorithm algorithm = JIPipe.createNode(MergeParametersAlgorithm.class);
        algorithm.getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.None);
        JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) algorithm.getSlotConfiguration();
        for (String slotName : ImmutableList.copyOf(algorithm.getInputSlotMap().keySet())) {
            configuration.removeInputSlot(slotName, false);
        }
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            configuration.addSlot(inputSlot.getName(), inputSlot.getInfo(), false);
            algorithm.getInputSlot(inputSlot.getName()).addDataFromSlot(inputSlot, progressInfo);
        }
        algorithm.run(runContext, progressInfo);
        getFirstOutputSlot().addDataFromSlot(algorithm.getFirstOutputSlot(), progressInfo);
    }
}
