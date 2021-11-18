package org.hkijena.jipipe.api.looping;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatchAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatchGenerationSettings;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithmDataBatchGenerationSettings;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatchBuilder;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@JIPipeDocumentation(name = "Loop start", description = "Indicates the start of a loop. All nodes following a loop start are " +
        "executed per data batch of this loop start node, unless its mode is set to pass-through. " +
        "All following nodes are assigned to a loop, unless a node has no output connections, or it is a loop end node. " +
        "Please be aware that intermediate results of this loop are discarded automatically, meaning that only the end points will contain the generated data. " +
        "You can also explicitly insert loop end nodes to collect results.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data")
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data")
public class LoopStartNode extends IOInterfaceAlgorithm implements JIPipeDataBatchAlgorithm {

    private GraphWrapperAlgorithm.IterationMode iterationMode = GraphWrapperAlgorithm.IterationMode.IteratingDataBatch;
    private JIPipeMergingAlgorithmDataBatchGenerationSettings batchGenerationSettings = new JIPipeMergingAlgorithmDataBatchGenerationSettings();

    public LoopStartNode(JIPipeNodeInfo info) {
        super(info);
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        slotConfiguration.addSlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, "Data"), true);
    }

    public LoopStartNode(LoopStartNode other) {
        super(other);
        this.iterationMode = other.iterationMode;
        this.batchGenerationSettings = new JIPipeMergingAlgorithmDataBatchGenerationSettings(other.batchGenerationSettings);
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

    @JIPipeDocumentation(name = "Data batch generation", description = "Only used if the graph iteration mode is not set to 'Pass data through'. " +
            "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true)
    public JIPipeMergingAlgorithmDataBatchGenerationSettings getBatchGenerationSettings() {
        return batchGenerationSettings;
    }

    @Override
    public JIPipeDataBatchGenerationSettings getGenerationSettingsInterface() {
        return batchGenerationSettings;
    }

    @Override
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots, JIPipeProgressInfo progressInfo) {
        if (iterationMode == GraphWrapperAlgorithm.IterationMode.PassThrough) {
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
            for (JIPipeDataSlot inputSlot : getEffectiveInputSlots()) {
                for (int row = 0; row < inputSlot.getRowCount(); row++) {
                    dataBatch.addInputData(inputSlot, row);
                }
            }
            return Collections.singletonList(dataBatch);
        } else {
            JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
            builder.setNode(this);
            builder.setApplyMerging(iterationMode == GraphWrapperAlgorithm.IterationMode.MergingDataBatch);
            builder.setSlots(slots);
            builder.setAnnotationMergeStrategy(batchGenerationSettings.getAnnotationMergeStrategy());
            builder.setReferenceColumns(batchGenerationSettings.getColumnMatching(),
                    batchGenerationSettings.getCustomColumns());
            builder.setCustomAnnotationMatching(batchGenerationSettings.getCustomAnnotationMatching());
            builder.setAnnotationMatchingMethod(batchGenerationSettings.getAnnotationMatchingMethod());
            List<JIPipeMergingDataBatch> dataBatches = builder.build(progressInfo);
            dataBatches.sort(Comparator.naturalOrder());
            boolean withLimit = batchGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = batchGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers()) : null;
            if (withLimit) {
                List<JIPipeMergingDataBatch> limitedBatches = new ArrayList<>();
                for (int i = 0; i < dataBatches.size(); i++) {
                    if (allowedIndices.contains(i)) {
                        limitedBatches.add(dataBatches.get(i));
                    }
                }
                dataBatches = limitedBatches;
            }
            if (batchGenerationSettings.isSkipIncompleteDataSets()) {
                dataBatches.removeIf(JIPipeMergingDataBatch::isIncomplete);
            }
            return dataBatches;
        }
    }
}