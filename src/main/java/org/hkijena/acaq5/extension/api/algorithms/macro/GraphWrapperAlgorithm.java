package org.hkijena.acaq5.extension.api.algorithms.macro;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterHolder;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterHolder;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An algorithm that wraps another algorithm graph
 */
public class GraphWrapperAlgorithm extends ACAQAlgorithm implements ACAQCustomParameterHolder {

    private ACAQAlgorithmGraph wrappedGraph;
    private Map<String, ACAQDataSlot> graphSlots = new HashMap<>();
    private Map<String, ACAQParameterAccess> parameterAccessMap = new HashMap<>();

    public GraphWrapperAlgorithm(GraphWrapperAlgorithmDeclaration declaration) {
        super(declaration, new ACAQMutableSlotConfiguration());
        this.wrappedGraph = new ACAQAlgorithmGraph(declaration.getGraph());

        initializeSlots();
        initializeParameters();
    }

    public GraphWrapperAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
        this.wrappedGraph = new ACAQAlgorithmGraph(other.wrappedGraph);
        initializeSlots();
        initializeParameters();
        for (Map.Entry<String, ACAQParameterAccess> entry : other.parameterAccessMap.entrySet()) {
            parameterAccessMap.get(entry.getKey()).set(entry.getValue().get());
        }
    }

    private void initializeParameters() {
        GraphWrapperAlgorithmDeclaration declaration = (GraphWrapperAlgorithmDeclaration)getDeclaration();

        for (ACAQAlgorithm algorithm : wrappedGraph.traverseAlgorithms()) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQParameterAccess.getParameters(algorithm).entrySet()) {

                String newId = algorithm.getIdInGraph() + "/" + entry.getKey();

                if(!declaration.getParameterCollectionVisibilities().isVisible(newId)) {
                    continue;
                }
                if(entry.getValue().getParameterHolder() instanceof ACAQDynamicParameterHolder) {
                    ((ACAQDynamicParameterHolder) entry.getValue().getParameterHolder()).setAllowModification(false);
                }

                parameterAccessMap.put(newId, entry.getValue());
            }
        }
    }

    private void initializeSlots() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)getSlotConfiguration();
        slotConfiguration.setInputSealed(false);
        slotConfiguration.setOutputSealed(false);
        slotConfiguration.clearInputSlots();
        slotConfiguration.clearOutputSlots();
        for (ACAQDataSlot slot : wrappedGraph.getUnconnectedSlots()) {
            if(slot.isInput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), " ", s -> slotConfiguration.getSlots().containsKey(s));
                slotConfiguration.addInputSlot(name, slot.getAcceptedDataType());
                graphSlots.put(name, slot);
            }
            else if(slot.isOutput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), " ", s -> slotConfiguration.getSlots().containsKey(s));
                slotConfiguration.addOutputSlot(name, slot.getAcceptedDataType());
                graphSlots.put(name, slot);
            }
        }
        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
    }

    @Override
    public void run() {
        transferInputData();

        List<ACAQDataSlot> traversedSlots = wrappedGraph.traverse();
        Set<ACAQAlgorithm> executedAlgorithms = new HashSet<>();

        for (int i = 0; i < traversedSlots.size(); ++i) {
            ACAQDataSlot slot = traversedSlots.get(i);

            if (slot.isInput()) {
                // Copy data from source
                ACAQDataSlot sourceSlot = wrappedGraph.getSourceSlot(slot);
                if(sourceSlot != null)
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
        clearWrappedGraphData();
    }

    private void clearWrappedGraphData() {
        for (ACAQDataSlot slot : wrappedGraph.traverse()) {
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

    @Override
    public Map<String, ACAQParameterAccess> getCustomParameters() {
        return parameterAccessMap;
    }

    public ACAQAlgorithmGraph getWrappedGraph() {
        return wrappedGraph;
    }
}
