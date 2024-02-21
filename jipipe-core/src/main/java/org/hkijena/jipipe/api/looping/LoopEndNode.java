package org.hkijena.jipipe.api.looping;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

@SetJIPipeDocumentation(name = "Loop end", description = "Deprecated. Use graph partitions instead. " + "Indicates the end of a loop. All nodes following a loop start are " +
        "executed per data batch of this loop start node")
@DefineJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Data")
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Data")
@LabelAsJIPipeHidden
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
