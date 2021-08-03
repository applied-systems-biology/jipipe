package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
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

@JIPipeDocumentation(name = "Split data randomly (percentage)", description = "Distributes data across the output slots, so a certain percentage of data is in the specified slot." +
        " Please note that negative weights will be replaced by zero. The output data is unique, meaning that there will be no overlaps between different slots.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Split")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class)
public class DistributeDataRandomlyByPercentageAlgorithm extends JIPipeMergingAlgorithm {
    private final OutputSlotMapParameterCollection weights;

    public DistributeDataRandomlyByPercentageAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", JIPipeData.class)
                .sealInput()
                .build());
        weights = new OutputSlotMapParameterCollection(Double.class, this, () -> 1, false);
        weights.updateSlots();
        registerSubParameter(weights);
    }

    public DistributeDataRandomlyByPercentageAlgorithm(DistributeDataRandomlyByPercentageAlgorithm other) {
        super(other);
        weights = new OutputSlotMapParameterCollection(Double.class, this, () -> 1, false);
        other.weights.copyTo(weights);
        registerSubParameter(weights);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
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
        List<Integer> availableRows = new ArrayList<>(dataBatch.getInputRows("Input"));
        Collections.shuffle(availableRows);
        for (Integer row : availableRows) {
            if (weightMap.isEmpty())
                return;
            String target = weightMap.keySet().iterator().next();
            double available = weightMap.get(target);
            if (available > 0) {
                getOutputSlot(target).addData(getFirstInputSlot().getVirtualData(row),
                        getFirstInputSlot().getAnnotations(row),
                        JIPipeAnnotationMergeStrategy.OverwriteExisting,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
            }
            --available;
            weightMap.put(target, available);
            if (available <= 0 && weightMap.size() != 1) {
                weightMap.remove(target);
            }
        }
    }

    @JIPipeDocumentation(name = "Weights", description = "Here you can set weights for each slot. They will be automatically converted into percentages.")
    @JIPipeParameter("percentages")
    public OutputSlotMapParameterCollection getWeights() {
        return weights;
    }

    @JIPipeDocumentation(name = "80:20 distribution", description = "Loads example parameters that distributes the outputs 80:20.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/distribute-randomize.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/distribute-randomize.png")
    public void setTo80And20Distribution(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
            slotConfiguration.clearOutputSlots(true);
            slotConfiguration.addSlot("W80", new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output), true);
            slotConfiguration.addSlot("W20", new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output), true);
            weights.get("W80").set(80.0);
            weights.get("W20").set(20.0);
        }
    }
}
