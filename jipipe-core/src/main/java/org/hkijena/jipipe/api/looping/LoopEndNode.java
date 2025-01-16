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

package org.hkijena.jipipe.api.looping;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

@SetJIPipeDocumentation(name = "Loop end", description = "Deprecated. Use graph partitions instead. " + "Indicates the end of a loop. All nodes following a loop start are " +
        "executed per iteration step of this loop start node")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data")
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Data")
@LabelAsJIPipeHidden
@Deprecated
public class LoopEndNode extends IOInterfaceAlgorithm {

    private JIPipeGraphWrapperAlgorithm.IterationMode iterationMode = JIPipeGraphWrapperAlgorithm.IterationMode.IteratingDataBatch;

    public LoopEndNode(JIPipeNodeInfo info) {
        super(info);
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        slotConfiguration.addSlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, "Data", ""), true);
    }

    public LoopEndNode(LoopEndNode other) {
        super(other);
        this.iterationMode = other.iterationMode;
    }
}
