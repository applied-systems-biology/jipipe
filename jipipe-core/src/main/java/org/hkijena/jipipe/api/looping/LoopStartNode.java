package org.hkijena.jipipe.api.looping;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@JIPipeDocumentation(name = "Loop start", description = "Indicates the start of a loop. All nodes following a loop start are " +
        "executed per data batch of this loop start node, unless its mode is set to pass-through. " +
        "All following nodes are assigned to a loop, unless a node has no output connections, or it is a loop end node. " +
        "Please be aware that intermediate results of this loop are discarded automatically, meaning that only the end points will contain the generated data. " +
        "You can also explicitly insert loop end nodes to collect results.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class LoopStartNode extends IOInterfaceAlgorithm {

    private GraphWrapperAlgorithm.IterationMode iterationMode = GraphWrapperAlgorithm.IterationMode.IteratingDataBatch;

    public LoopStartNode(JIPipeNodeInfo info) {
        super(info);
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        slotConfiguration.addSlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, "Data"), true);
    }

    public LoopStartNode(LoopStartNode other) {
        super(other);
        this.iterationMode = other.iterationMode;
    }

    @JIPipeDocumentation(name = "Iteration mode", description = "Determines how the loop is iterated:" +
            "<ul>" +
            "<li>Pass through: Disables looping. The node behaves as a regular IO interface.</li>" +
            "<li>The loop can be executed per data batch. Here you can choose between an iterative data batch (one item per slot) " +
            "or a merging data batch (multiple items per slot).</li>" +
            "</ul>")
    @JIPipeParameter("iteration-mode")
    public GraphWrapperAlgorithm.IterationMode getIterationMode() {
        return iterationMode;
    }

    @JIPipeParameter("iteration-mode")
    public void setIterationMode(GraphWrapperAlgorithm.IterationMode iterationMode) {
       this.iterationMode = iterationMode;
    }
}
