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

package org.hkijena.jipipe.plugins.utils.algorithms.distribute;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.plugins.parameters.library.graph.OutputSlotMapParameterCollection;

import java.util.*;

@SetJIPipeDocumentation(name = "Split data randomly (count)", description = "Distributes data across the output slots, so a each of the slot is provided with the number of data as specified. " +
        "The output data is unique, meaning that there will be no overlaps between different slots. If there is not enough data available, the slots with the lower order (left/top) are preferred.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class)
public class DistributeDataRandomlyByCountAlgorithm extends JIPipeMergingAlgorithm {
    private final OutputSlotMapParameterCollection counts;

    public DistributeDataRandomlyByCountAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", JIPipeData.class)
                .sealInput()
                .build());
        getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.MergeAll);
        counts = new OutputSlotMapParameterCollection(Integer.class, this, (slotInfo) -> 1, false);
        counts.updateSlots();
        registerSubParameter(counts);
    }

    public DistributeDataRandomlyByCountAlgorithm(DistributeDataRandomlyByCountAlgorithm other) {
        super(other);
        counts = new OutputSlotMapParameterCollection(Integer.class, this, (slotInfo) -> 1, false);
        other.counts.copyTo(counts);
        registerSubParameter(counts);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (getInputSlots().isEmpty())
            return;
        Map<String, Integer> countMap = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : counts.getParameters().entrySet()) {
            countMap.put(entry.getKey(), entry.getValue().get(Integer.class));
        }
        // Generate random order
        List<Integer> availableRows = new ArrayList<>(iterationStep.getInputRows("Input"));
        Collections.shuffle(availableRows);
        for (Integer row : availableRows) {
            if (countMap.isEmpty())
                return;
            String target = countMap.keySet().iterator().next();
            int available = countMap.get(target);
            if (available > 0) {
                // Add output with restoring annotations
                getOutputSlot(target).addData(getFirstInputSlot().getDataItemStore(row),
                        getFirstInputSlot().getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataContext(row).branch(this),
                        progressInfo);
            }
            --available;
            countMap.put(target, available);
            if (available <= 0 && countMap.size() != 1) {
                countMap.remove(target);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Counts", description = "Here you can set output counts for each slot.")
    @JIPipeParameter("counts")
    public OutputSlotMapParameterCollection getCounts() {
        return counts;
    }
}
