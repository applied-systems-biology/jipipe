package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.collections.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Split data randomly (count)", description = "Distributes data across the output slots, so a each of the slot is provided with the number of data as specified. " +
        "The output data is unique, meaning that there will be no overlaps between different slots. If there is not enough data available, the slots with the lower order (left/top) are preferred.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class)
public class DistributeDataRandomlyByCountAlgorithm extends JIPipeMergingAlgorithm {
    private final OutputSlotMapParameterCollection counts;

    public DistributeDataRandomlyByCountAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", JIPipeData.class)
                .sealInput()
                .build());
        counts = new OutputSlotMapParameterCollection(Integer.class, this, () -> 1, false);
        counts.updateSlots();
        registerSubParameter(counts);
    }

    public DistributeDataRandomlyByCountAlgorithm(DistributeDataRandomlyByCountAlgorithm other) {
        super(other);
        counts = new OutputSlotMapParameterCollection(Integer.class, this, () -> 1, false);
        other.counts.copyTo(counts);
        registerSubParameter(counts);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (getInputSlots().isEmpty())
            return;
        Map<String, Integer> countMap = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : counts.getParameters().entrySet()) {
            countMap.put(entry.getKey(), entry.getValue().get(Integer.class));
        }
        // Generate random order
        List<Integer> availableRows = new ArrayList<>(dataBatch.getInputRows("Input"));
        Collections.shuffle(availableRows);
        for (Integer row : availableRows) {
            if (countMap.isEmpty())
                return;
            String target = countMap.keySet().iterator().next();
            int available = countMap.get(target);
            if (available > 0) {
                // Add output with restoring annotations
                getOutputSlot(target).addData(getFirstInputSlot().getVirtualData(row),
                        getFirstInputSlot().getAnnotations(row),
                        JIPipeAnnotationMergeStrategy.OverwriteExisting,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
            }
            --available;
            countMap.put(target, available);
            if (available <= 0 && countMap.size() != 1) {
                countMap.remove(target);
            }
        }
    }

    @JIPipeDocumentation(name = "Counts", description = "Here you can set output counts for each slot.")
    @JIPipeParameter("counts")
    public OutputSlotMapParameterCollection getCounts() {
        return counts;
    }
}
