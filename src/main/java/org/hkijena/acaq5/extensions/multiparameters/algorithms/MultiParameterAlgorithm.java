package org.hkijena.acaq5.extensions.multiparameters.algorithms;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclarationRef;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.data.traits.ACAQTraitTransferTask;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An algorithm that takes multi parameters
 */
public class MultiParameterAlgorithm extends ACAQAlgorithm {

    private ACAQAlgorithm algorithmInstance;

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public MultiParameterAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new MultiParameterAlgorithmSlotConfiguration());
        ((MultiParameterAlgorithmSlotConfiguration) getSlotConfiguration()).setAlgorithmInstance(algorithmInstance);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MultiParameterAlgorithm(MultiParameterAlgorithm other) {
        super(other);
        MultiParameterAlgorithmSlotConfiguration slotConfiguration = new MultiParameterAlgorithmSlotConfiguration();
        this.setSlotConfiguration(slotConfiguration);
        slotConfiguration.setTo(other.getSlotConfiguration());
        if (other.algorithmInstance != null) {
            this.algorithmInstance = other.algorithmInstance.getDeclaration().clone(other.algorithmInstance);
            this.algorithmInstance.getEventBus().register(this);
        }
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        checkInputSlots();

        // Backup default parameters
        Map<String, Object> defaultParameterSnapshot = getDefaultParameterSnapshot();
        ACAQDataSlot parameterSlot = getInputSlot("Parameters");
        Map<String, ACAQParameterAccess> parameters = ACAQParameterCollection.getParameters(algorithmInstance);
        Map<String, ACAQTraitDeclaration> changedParameterTraits = new HashMap<>();

        for (int row = 0; row < parameterSlot.getRowCount(); ++row) {
            if (isCancelled.get())
                return;
            ParametersData parametersData = parameterSlot.getData(row, ParametersData.class);
            for (Map.Entry<String, Object> entry : parametersData.getParameterData().entrySet()) {
                ACAQParameterAccess parameterAccess = parameters.getOrDefault(entry.getKey(), null);
                if (parameterAccess == null) {
                    throw new NullPointerException("Parameter with key '" + entry.getKey() + "' was no found in " + algorithmInstance);
                }
                if (!parameterAccess.getFieldClass().isAssignableFrom(entry.getValue().getClass())) {
                    throw new UnsupportedOperationException("Cannot assign parameter '" + entry.getKey() +
                            "' and value '" + entry.getValue() + "' to " + algorithmInstance);
                }
                if (!Objects.equals(parameterAccess.get(), entry.getValue())) {
                    String traitId = "parameter-" + StringUtils.jsonify(parameterAccess.getName());
                    if (ACAQTraitRegistry.getInstance().hasTraitWithId(traitId)) {
                        changedParameterTraits.put(entry.getKey(), ACAQTraitRegistry.getInstance().getDeclarationById(traitId));
                    }
                }
            }
        }

        // Run algorithm for each parameter
        for (int row = 0; row < parameterSlot.getRowCount(); ++row) {
            if (isCancelled.get())
                return;
            ACAQRunnerSubStatus parameterProgress = subProgress.resolve("Parameter set " + (row + 1) + " / " + parameterSlot.getRowCount());
            ParametersData parametersData = parameterSlot.getData(row, ParametersData.class);

            passParameters(parametersData, parameters);
            passInputData();
            algorithmInstance.run(parameterProgress, algorithmProgress, isCancelled);
            passOutputData(parameters, changedParameterTraits);

            // Restore backup
            for (Map.Entry<String, Object> entry : defaultParameterSnapshot.entrySet()) {
                parameters.get(entry.getKey()).set(entry.getValue());
            }
        }
    }

    private Map<String, Object> getDefaultParameterSnapshot() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, ACAQParameterAccess> entry : ACAQParameterCollection.getParameters(algorithmInstance).entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }

    private void passOutputData(Map<String, ACAQParameterAccess> parameters, Map<String, ACAQTraitDeclaration> newParameters) {
        for (ACAQDataSlot wrappedOutputSlot : algorithmInstance.getOutputSlots()) {
            ACAQDataSlot outputSlot = getOutputSlot("Data " + wrappedOutputSlot.getName());
            for (int row = 0; row < wrappedOutputSlot.getRowCount(); ++row) {
                ACAQData data = wrappedOutputSlot.getData(row, ACAQData.class);
                List<ACAQTrait> traits = wrappedOutputSlot.getAnnotations(row);
                for (Map.Entry<String, ACAQTraitDeclaration> entry : newParameters.entrySet()) {
                    traits.add(entry.getValue().newInstance("" + parameters.get(entry.getKey()).get()));
                }
                outputSlot.addData(data, traits);
            }
            wrappedOutputSlot.clearData();
        }
    }

    private void passInputData() {
        for (ACAQDataSlot wrappedInputSlot : algorithmInstance.getInputSlots()) {
            ACAQDataSlot inputSlot = getInputSlot("Data " + wrappedInputSlot.getName());
            wrappedInputSlot.clearData();
            wrappedInputSlot.copyFrom(inputSlot);
        }
    }

    private void passParameters(ParametersData parametersData, Map<String, ACAQParameterAccess> parameters) {
        for (Map.Entry<String, Object> entry : parametersData.getParameterData().entrySet()) {
            ACAQParameterAccess parameterAccess = parameters.getOrDefault(entry.getKey(), null);
            if (parameterAccess == null) {
                throw new NullPointerException("Parameter with key '" + entry.getKey() + "' was no found in " + algorithmInstance);
            }
            if (!parameterAccess.getFieldClass().isAssignableFrom(entry.getValue().getClass())) {
                throw new UnsupportedOperationException("Cannot assign parameter '" + entry.getKey() +
                        "' and value '" + entry.getValue() + "' to " + algorithmInstance);
            }
            parameterAccess.set(entry.getValue());
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (algorithmInstance == null) {
            report.reportIsInvalid("No algorithm selected! Please select an algorithm in the parameter panel.");
        }
    }

    public List<ACAQDataSlot> getDataInputSlots() {
        return getInputSlots().stream().filter(s -> s.getName().startsWith("Data ")).collect(Collectors.toList());
    }

    private void checkInputSlots() {
        List<ACAQDataSlot> dataInputSlots = getDataInputSlots();
        if (dataInputSlots.isEmpty())
            return;
        int rows = dataInputSlots.get(0).getRowCount();
        for (int i = 1; i < dataInputSlots.size(); ++i) {
            if (rows != dataInputSlots.get(i).getRowCount())
                throw new RuntimeException("Data input slots have a different row count!");
        }
    }

    @ACAQParameter("algorithm-type")
    @ACAQDocumentation(name = "Algorithm", description = "The algorithm the parameters are created for")
    public ACAQAlgorithmDeclarationRef getAlgorithmDeclaration() {
        if (algorithmInstance == null) {
            return new ACAQAlgorithmDeclarationRef();
        } else {
            return new ACAQAlgorithmDeclarationRef(algorithmInstance.getDeclaration());
        }
    }

    @ACAQParameter("algorithm-type")
    public void setAlgorithmDeclaration(ACAQAlgorithmDeclarationRef algorithmDeclaration) {
        if (algorithmDeclaration.getDeclaration() != null) {
            if (algorithmInstance != null) {
                algorithmInstance.getEventBus().unregister(this);
            }
            algorithmInstance = algorithmDeclaration.getDeclaration().newInstance();
        } else {
            algorithmInstance = null;
        }
        updateAlgorithmInstance();
    }

    @ACAQSubParameters("algorithm-parameters")
    public ACAQAlgorithm getAlgorithmInstance() {
        return algorithmInstance;
    }

    private void updateAlgorithmInstance() {
        algorithmInstance.getEventBus().register(this);
        getEventBus().post(new ParameterStructureChangedEvent(this));

        ((MultiParameterAlgorithmSlotConfiguration) getSlotConfiguration()).setAlgorithmInstance(algorithmInstance);

        // Update trait configuration
        ACAQDefaultMutableTraitConfiguration traitConfiguration = (ACAQDefaultMutableTraitConfiguration) getTraitConfiguration();
        traitConfiguration.clear();

        if (algorithmInstance.getTraitConfiguration() instanceof ACAQDefaultMutableTraitConfiguration) {
            ACAQDefaultMutableTraitConfiguration other = (ACAQDefaultMutableTraitConfiguration) algorithmInstance.getTraitConfiguration();
            traitConfiguration.setTraitModificationsSealed(false);
            traitConfiguration.setTraitTransfersSealed(false);

            // Copy data over
            traitConfiguration.setTransferAllToAll(other.isTransferAllToAll());
            for (ACAQTraitTransferTask transferTask : other.getTransferTasks()) {
                traitConfiguration.addTransfer(new ACAQTraitTransferTask("Data " + transferTask.getInputSlotName(),
                        "Data " + transferTask.getOutputSlotName(),
                        new HashSet<>(transferTask.getTraitRestrictions())));
            }
            for (Map.Entry<ACAQTraitDeclaration, ACAQTraitModificationOperation> entry : other.getMutableGlobalTraitModificationTasks().getOperations().entrySet()) {
                traitConfiguration.getMutableGlobalTraitModificationTasks().getOperations().put(entry.getKey(), entry.getValue());
            }
            // TODO: Slot modification tasks


            traitConfiguration.setTraitTransfersSealed(other.isTraitTransfersSealed());
            traitConfiguration.setTraitModificationsSealed(other.isTraitModificationsSealed());
        }
    }

    /**
     * Triggered when the parameter structure of algorithm parameters is changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        getEventBus().post(event);
    }
}
