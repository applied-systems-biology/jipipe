package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQTraversedParameterCollection;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that wraps another algorithm graph
 */
public class GraphWrapperAlgorithm extends ACAQAlgorithm implements ACAQCustomParameterCollection {

    private ACAQAlgorithmGraph wrappedGraph;
    private Map<String, ACAQDataSlot> graphSlots = new HashMap<>();
    private Map<String, ACAQParameterAccess> parameterAccessMap = new HashMap<>();

    /**
     * @param declaration the declaration
     */
    public GraphWrapperAlgorithm(GraphWrapperAlgorithmDeclaration declaration) {
        super(declaration, new ACAQMutableSlotConfiguration());
        this.wrappedGraph = new ACAQAlgorithmGraph(declaration.getGraph());

        initializeSlots();
        initializeParameters();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public GraphWrapperAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
        this.wrappedGraph = new ACAQAlgorithmGraph(other.wrappedGraph);
        initializeSlots();
        initializeParameters();
        for (Map.Entry<String, ACAQParameterAccess> entry : other.parameterAccessMap.entrySet()) {
            parameterAccessMap.get(entry.getKey()).set(entry.getValue().get(Object.class));
        }
    }

    private void initializeParameters() {
        GraphWrapperAlgorithmDeclaration declaration = (GraphWrapperAlgorithmDeclaration) getDeclaration();
        ACAQTraversedParameterCollection parameterCollection = new ACAQTraversedParameterCollection();
        parameterCollection.setForceReflection(true);
        parameterCollection.add(this, Collections.emptyList());
        parameterAccessMap.putAll(parameterCollection.getParameters());

        for (ACAQAlgorithm algorithm : wrappedGraph.traverseAlgorithms()) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQTraversedParameterCollection.getParameters(algorithm).entrySet()) {

                ACAQParameterAccess parameterAccess = entry.getValue();
                String newId = algorithm.getIdInGraph() + "/" + entry.getKey();

                if (!declaration.getParameterCollectionVisibilities().isVisible(newId)) {
                    continue;
                }
                if (parameterAccess.getSource() instanceof ACAQDynamicParameterCollection) {
                    ((ACAQDynamicParameterCollection) parameterAccess.getSource()).setAllowUserModification(false);
                }

                parameterAccessMap.put(newId, entry.getValue());
            }
        }
    }

    private void initializeSlots() {
        graphSlots.clear();
        for (Map.Entry<ACAQDataSlot, String> entry : ((GraphWrapperAlgorithmDeclaration) getDeclaration()).getExportedSlotNames().entrySet()) {
            ACAQAlgorithm localAlgorithm = wrappedGraph.getEquivalentAlgorithm(entry.getKey().getAlgorithm());
            ACAQDataSlot localSlot = localAlgorithm.getSlots().get(entry.getKey().getName());
            graphSlots.put(entry.getValue(), localSlot);
        }

        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getSlotConfiguration();
        slotConfiguration.setInputSealed(false);
        slotConfiguration.setOutputSealed(false);
        slotConfiguration.setAllowInheritedOutputSlots(true);
        slotConfiguration.clearInputSlots();
        slotConfiguration.clearOutputSlots();
        for (AlgorithmInputSlot slot : getDeclaration().getInputSlots()) {
            slotConfiguration.addSlot(slot.slotName(), new ACAQSlotDefinition(slot.value(), ACAQDataSlot.SlotType.Input, slot.slotName(), null), false);
        }
        for (AlgorithmOutputSlot slot : getDeclaration().getOutputSlots()) {
            slotConfiguration.addSlot(slot.slotName(), new ACAQSlotDefinition(slot.value(), ACAQDataSlot.SlotType.Output, slot.slotName(), null), false);
        }
        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
    }

    @Override
    public void updateSlotInheritance() {
        if (getGraph() != null) {
            // Pass the input type to the graph inputs
            // Then propagate the change
            // Then change the data type back
            Set<ACAQAlgorithm> graphInputAlgorithms = new HashSet<>();
            for (Map.Entry<String, ACAQDataSlot> entry : graphSlots.entrySet()) {
                if (entry.getValue().isInput()) {
                    graphInputAlgorithms.add(entry.getValue().getAlgorithm());
                    ACAQDataSlot globalSlot = getInputSlot(entry.getKey());
                    Class<? extends ACAQData> dataClass = getSlotConfiguration().getSlots().get(entry.getKey()).getDataClass();
                    ACAQDataSlot globalSource = getGraph().getSourceSlot(globalSlot);
                    if (globalSource != null) {
                        dataClass = globalSource.getAcceptedDataType();
                    }
                    entry.getValue().setAcceptedDataType(dataClass);
                }
            }
            for (ACAQAlgorithm graphInputAlgorithm : graphInputAlgorithms) {
                graphInputAlgorithm.updateSlotInheritance();
            }
            for (Map.Entry<String, ACAQDataSlot> entry : graphSlots.entrySet()) {
                if (entry.getValue().isInput()) {
                    Class<? extends ACAQData> dataClass = getSlotConfiguration().getSlots().get(entry.getKey()).getDataClass();
                    entry.getValue().setAcceptedDataType(dataClass);
                }
            }

            // Pass the output data type to the global outputs
            boolean modified = false;
            for (Map.Entry<String, ACAQDataSlot> entry : graphSlots.entrySet()) {
                ACAQDataSlot slotInstance = getSlots().getOrDefault(entry.getKey(), null);
                if (slotInstance == null || slotInstance.getSlotType() != ACAQDataSlot.SlotType.Output)
                    continue;

                Class<? extends ACAQData> expectedSlotDataType = entry.getValue().getAcceptedDataType();
                if (slotInstance.getAcceptedDataType() != expectedSlotDataType) {
                    slotInstance.setAcceptedDataType(expectedSlotDataType);
                    getEventBus().post(new AlgorithmSlotsChangedEvent(this));
                    modified = true;
                }
            }
            if (modified) {
                Set<ACAQAlgorithm> algorithms = new HashSet<>();
                for (ACAQDataSlot slot : getOutputSlots()) {
                    for (ACAQDataSlot targetSlot : getGraph().getTargetSlots(slot)) {
                        algorithms.add(targetSlot.getAlgorithm());
                    }
                }
                for (ACAQAlgorithm algorithm : algorithms) {
                    algorithm.updateSlotInheritance();
                }
            }
        }
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        transferInputData();

        List<ACAQDataSlot> traversedSlots = wrappedGraph.traverse();
        Set<ACAQAlgorithm> executedAlgorithms = new HashSet<>();

        for (int i = 0; i < traversedSlots.size(); ++i) {
            ACAQDataSlot slot = traversedSlots.get(i);

            if (slot.isInput()) {
                // Copy data from source
                ACAQDataSlot sourceSlot = wrappedGraph.getSourceSlot(slot);
                if (sourceSlot != null)
                    slot.copyFrom(sourceSlot);
            } else if (slot.isOutput()) {
                // Ensure the algorithm has run
                if (!executedAlgorithms.contains(slot.getAlgorithm())) {
                    slot.getAlgorithm().run(subProgress.resolve(slot.getAlgorithm().getName()), algorithmProgress, isCancelled);
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
            if (entry.getValue().isOutput()) {
                ACAQDataSlot sourceSlot = entry.getValue();
                ACAQDataSlot targetSlot = getOutputSlot(entry.getKey());
                for (int row = 0; row < sourceSlot.getRowCount(); ++row) {
                    targetSlot.addData(sourceSlot.getData(row, ACAQData.class), sourceSlot.getAnnotations(row));
                }
            }
        }
    }

    private void transferInputData() {
        for (Map.Entry<String, ACAQDataSlot> entry : graphSlots.entrySet()) {
            if (entry.getValue().isInput()) {
                ACAQDataSlot targetSlot = entry.getValue();
                ACAQDataSlot sourceSlot = getInputSlot(entry.getKey());
                for (int row = 0; row < sourceSlot.getRowCount(); ++row) {
                    targetSlot.addData(sourceSlot.getData(row, ACAQData.class), sourceSlot.getAnnotations(row));
                }
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = super.getDependencies();
        result.addAll(wrappedGraph.getDependencies());
        return result;
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        return parameterAccessMap;
    }

    public ACAQAlgorithmGraph getWrappedGraph() {
        return wrappedGraph;
    }
}
