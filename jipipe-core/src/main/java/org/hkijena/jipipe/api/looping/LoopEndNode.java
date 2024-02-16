package org.hkijena.jipipe.api.looping;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

@JIPipeDocumentation(name = "Loop end", description = "Indicates the end of a loop. All nodes following a loop start are " +
        "executed per data batch of this loop start node")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data")
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data")
@JIPipeHidden
@Deprecated
public class LoopEndNode extends IOInterfaceAlgorithm {

    private GraphWrapperAlgorithm.IterationMode iterationMode = GraphWrapperAlgorithm.IterationMode.IteratingDataBatch;

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
