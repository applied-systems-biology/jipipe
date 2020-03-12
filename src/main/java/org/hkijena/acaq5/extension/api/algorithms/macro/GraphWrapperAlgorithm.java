package org.hkijena.acaq5.extension.api.algorithms.macro;

import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An algorithm that wraps another algorithm graph
 */
public class GraphWrapperAlgorithm extends ACAQAlgorithm {

    private ACAQAlgorithmGraph graph;
    private Map<String, ACAQDataSlot> graphSlots = new HashMap<>();

    public GraphWrapperAlgorithm(GraphWrapperAlgorithmDeclaration declaration) {
        super(declaration, new ACAQMutableSlotConfiguration());
        this.graph = new ACAQAlgorithmGraph(declaration.getGraph());

        initializeSlots();
    }

    private void initializeSlots() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)getSlotConfiguration();
        slotConfiguration.setInputSealed(false);
        slotConfiguration.setOutputSealed(false);
        for (ACAQDataSlot slot : graph.getUnconnectedSlots()) {
            if(slot.isInput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), s -> slotConfiguration.getSlots().containsKey(s));
                slotConfiguration.addInputSlot(name, slot.getAcceptedDataType());
                graphSlots.put(name, slot);
            }
            else if(slot.isOutput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), s -> slotConfiguration.getSlots().containsKey(s));
                slotConfiguration.addOutputSlot(name, slot.getAcceptedDataType());
                graphSlots.put(name, slot);
            }
        }
        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
    }

    public GraphWrapperAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
        this.graph = new ACAQAlgorithmGraph(other.getGraph());
        initializeSlots();
    }

    @Override
    public void run() {
        transferInputData();

        List<ACAQDataSlot> traversedSlots = graph.traverse();
        Set<ACAQAlgorithm> executedAlgorithms = new HashSet<>();

        for (int i = 0; i < traversedSlots.size(); ++i) {
            ACAQDataSlot slot = traversedSlots.get(i);

            if (slot.isInput()) {
                // Copy data from source
                ACAQDataSlot sourceSlot = graph.getSourceSlot(slot);
                slot.copyFrom(sourceSlot);
            } else if (slot.isOutput()) {
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getAlgorithm())) {
                    slot.getAlgorithm().run();
                    executedAlgorithms.add(slot.getAlgorithm());
                }
            }
        }
        
        transferOutputData();
        clearGraphData();
    }

    private void clearGraphData() {
        for (ACAQDataSlot slot : graph.traverse()) {
            slot.clearData();
        }
    }

    private void transferOutputData() {
        for (Map.Entry<String, ACAQDataSlot> entry : graphSlots.entrySet()) {
            if(entry.getValue().isOutput()) {
                ACAQDataSlot sourceSlot = entry.getValue();
                ACAQDataSlot targetSlot = getOutputSlot(entry.getKey());
                for(int row = 0; row < sourceSlot.getRowCount(); ++row) {
                    targetSlot.addData(sourceSlot.getData(row), sourceSlot.getAnnotations(row));
                }
            }
        }
    }

    private void transferInputData() {
        for (Map.Entry<String, ACAQDataSlot> entry : graphSlots.entrySet()) {
            if(entry.getValue().isInput()) {
                ACAQDataSlot targetSlot = entry.getValue();
                ACAQDataSlot sourceSlot = getInputSlot(entry.getKey());
                for(int row = 0; row < sourceSlot.getRowCount(); ++row) {
                    targetSlot.addData(sourceSlot.getData(row), sourceSlot.getAnnotations(row));
                }
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
