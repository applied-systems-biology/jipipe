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

package org.hkijena.jipipe.extensions.utils.algorithms.distribute;

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
import org.hkijena.jipipe.extensions.parameters.library.graph.OutputSlotMapParameterCollection;

import java.util.*;

@SetJIPipeDocumentation(name = "Split data randomly (percentage)", description = "Distributes data across the output slots, so a certain percentage of data is in the specified slot." +
        " Please note that negative weights will be replaced by zero. The output data is unique, meaning that there will be no overlaps between different slots.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class)
public class DistributeDataRandomlyByPercentageAlgorithm extends JIPipeMergingAlgorithm {
    private final OutputSlotMapParameterCollection weights;

    public DistributeDataRandomlyByPercentageAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", JIPipeData.class)
                .sealInput()
                .build());
        getDataBatchGenerationSettings().setColumnMatching(JIPipeColumMatching.MergeAll);
        weights = new OutputSlotMapParameterCollection(Double.class, this, (slotInfo) -> 1, false);
        weights.updateSlots();
        registerSubParameter(weights);
    }

    public DistributeDataRandomlyByPercentageAlgorithm(DistributeDataRandomlyByPercentageAlgorithm other) {
        super(other);
        weights = new OutputSlotMapParameterCollection(Double.class, this, (slotInfo) -> 1, false);
        other.weights.copyTo(weights);
        registerSubParameter(weights);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (getInputSlots().isEmpty())
            return;
        Map<String, Double> weightMap = new HashMap<>();
        double weightSum = 0;
        for (Map.Entry<String, JIPipeParameterAccess> entry : weights.getParameters().entrySet()) {
            double w = Math.max(0, entry.getValue().get(Double.class));
            weightMap.put(entry.getKey(), w);
            weightSum += w;
        }
        if (weightSum == 0) {
            // Fallback to 0
            for (String name : getOutputSlotMap().keySet()) {
                weightMap.put(name, 0.0);
            }
        } else {
            for (String name : getOutputSlotMap().keySet()) {
                weightMap.put(name, weightMap.get(name) / weightSum * getFirstInputSlot().getRowCount()); // Turn this into an absolute available count
            }
        }
        // Generate random order
        List<Integer> availableRows = new ArrayList<>(iterationStep.getInputRows("Input"));
        Collections.shuffle(availableRows);
        for (Integer row : availableRows) {
            if (weightMap.isEmpty())
                return;
            String target = weightMap.keySet().iterator().next();
            double available = weightMap.get(target);
            if (available > 0) {
                getOutputSlot(target).addData(getFirstInputSlot().getDataItemStore(row),
                        getFirstInputSlot().getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataContext(row).branch(this),
                        progressInfo);
            }
            --available;
            weightMap.put(target, available);
            if (available <= 0 && weightMap.size() != 1) {
                weightMap.remove(target);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Weights", description = "Here you can set weights for each slot. They will be automatically converted into percentages.")
    @JIPipeParameter("percentages")
    public OutputSlotMapParameterCollection getWeights() {
        return weights;
    }

}
