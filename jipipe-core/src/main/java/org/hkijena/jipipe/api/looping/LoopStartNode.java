package org.hkijena.jipipe.api.looping;

import com.google.common.primitives.Ints;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.ArrayList;
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
public class LoopStartNode extends IOInterfaceAlgorithm implements JIPipeIterationStepAlgorithm {

    private GraphWrapperAlgorithm.IterationMode iterationMode = GraphWrapperAlgorithm.IterationMode.IteratingDataBatch;
    private JIPipeMergingAlgorithmIterationStepGenerationSettings batchGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings();

    public LoopStartNode(JIPipeNodeInfo info) {
        super(info);
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
        slotConfiguration.addSlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, "Data", ""), true);
    }

    public LoopStartNode(LoopStartNode other) {
        super(other);
        this.iterationMode = other.iterationMode;
        this.batchGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings(other.batchGenerationSettings);
    }

    @JIPipeDocumentation(name = "Iteration mode", description = "Determines how the loop is iterated:" +
            "<ul>" +
            "<li>Pass through: Disables looping. The node behaves as a regular IO interface.</li>" +
            "<li>The loop can be executed per data batch. Here you can choose between an iterative data batch (one item per slot) " +
            "or a merging data batch (multiple items per slot).</li>" +
            "</ul><br/>" +
            "<strong>Automatically assumed to be 'Pass through', if the node is set to 'Pass through'</strong>")
    @JIPipeParameter("iteration-mode")
    public GraphWrapperAlgorithm.IterationMode getIterationMode() {
        return iterationMode;
    }

    @JIPipeParameter("iteration-mode")
    public void setIterationMode(GraphWrapperAlgorithm.IterationMode iterationMode) {
        this.iterationMode = iterationMode;
    }

    @JIPipeDocumentation(name = "Input management", description = "Only used if the graph iteration mode is not set to 'Pass data through'. " +
            "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true)
    public JIPipeMergingAlgorithmIterationStepGenerationSettings getBatchGenerationSettings() {
        return batchGenerationSettings;
    }

    @Override
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return batchGenerationSettings;
    }

    @Override
    public JIPipeDataBatchGenerationResult generateDataBatchesGenerationResult(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        if (iterationMode == GraphWrapperAlgorithm.IterationMode.PassThrough) {
            JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
            for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
                for (int row = 0; row < inputSlot.getRowCount(); row++) {
                    iterationStep.addInputData(inputSlot, row);
                }
            }

            // Generate result object
            JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
            result.setDataBatches(iterationStep);

            return result;
        } else {
            JIPipeMultiIterationStepGenerator builder = new JIPipeMultiIterationStepGenerator();
            builder.setNode(this);
            builder.setApplyMerging(iterationMode == GraphWrapperAlgorithm.IterationMode.MergingDataBatch);
            builder.setSlots(slots);
            builder.setAnnotationMergeStrategy(batchGenerationSettings.getAnnotationMergeStrategy());
            builder.setReferenceColumns(batchGenerationSettings.getColumnMatching(),
                    batchGenerationSettings.getCustomColumns());
            builder.setCustomAnnotationMatching(batchGenerationSettings.getCustomAnnotationMatching());
            builder.setAnnotationMatchingMethod(batchGenerationSettings.getAnnotationMatchingMethod());
            builder.setForceFlowGraphSolver(batchGenerationSettings.isForceFlowGraphSolver());
            List<JIPipeMultiIterationStep> iterationSteps = builder.build(progressInfo);
            iterationSteps.sort(Comparator.naturalOrder());
            boolean withLimit = batchGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = batchGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, iterationSteps.size(), new JIPipeExpressionVariablesMap())) : null;
            if (withLimit) {
                progressInfo.log("[INFO] Applying limit to all data batches. Allowed indices are " + Ints.join(", ", allowedIndices.toArray()));
                List<JIPipeMultiIterationStep> limitedBatches = new ArrayList<>();
                for (int i = 0; i < iterationSteps.size(); i++) {
                    if (allowedIndices.contains(i)) {
                        limitedBatches.add(iterationSteps.get(i));
                    }
                }
                iterationSteps = limitedBatches;
            }
            List<JIPipeMultiIterationStep> incomplete = new ArrayList<>();
            for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
                if (iterationStep.isIncomplete()) {
                    incomplete.add(iterationStep);
                    progressInfo.log("[WARN] INCOMPLETE DATA BATCH FOUND: " + iterationStep);
                }
            }
            if (!incomplete.isEmpty() && batchGenerationSettings.isSkipIncompleteDataSets()) {
                progressInfo.log("[WARN] SKIPPING INCOMPLETE DATA BATCHES AS REQUESTED");
                iterationSteps.removeAll(incomplete);
            }

            // Generate result object
            JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
            result.setDataBatches(iterationSteps);
            result.setReferenceTextAnnotationColumns(builder.getReferenceColumns());

            return result;
        }
    }
}
