package org.hkijena.acaq5.extensions.multiparameters.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.acaq5.extensions.parameters.references.ACAQAlgorithmDeclarationRef;
import org.hkijena.acaq5.utils.ReflectionUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An algorithm that takes multi parameters
 */
public class MultiParameterAlgorithm extends ACAQAlgorithm {

    private ACAQGraphNode algorithmInstance;

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
        Map<String, ACAQParameterAccess> parameters = ACAQParameterTree.getParameters(algorithmInstance);
        Map<String, String> changedParameterTraits = new HashMap<>();

        for (int row = 0; row < parameterSlot.getRowCount(); ++row) {
            if (isCancelled.get())
                return;
            ParametersData parametersData = parameterSlot.getData(row, ParametersData.class);
            for (Map.Entry<String, Object> entry : parametersData.getParameterData().entrySet()) {
                ACAQParameterAccess parameterAccess = parameters.getOrDefault(entry.getKey(), null);
                if (parameterAccess == null) {
                    throw new NullPointerException("Parameter with key '" + entry.getKey() + "' was no found in " + algorithmInstance);
                }
                if (!ReflectionUtils.isAssignableTo(entry.getValue().getClass(), parameterAccess.getFieldClass())) {
                    throw new UnsupportedOperationException("Cannot assign parameter '" + entry.getKey() +
                            "' and value '" + entry.getValue() + "' to " + algorithmInstance);
                }
                if (!Objects.equals(parameterAccess.get(Object.class), entry.getValue())) {
                    String traitId = "parameter-" + StringUtils.jsonify(parameterAccess.getName());
                    changedParameterTraits.put(entry.getKey(), traitId);
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
        ACAQParameterTree collection = new ACAQParameterTree(algorithmInstance);
        for (Map.Entry<String, ACAQParameterAccess> entry : collection.getParameters().entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(Object.class));
        }
        return result;
    }

    private void passOutputData(Map<String, ACAQParameterAccess> parameters, Map<String, String> newParameters) {
        for (ACAQDataSlot wrappedOutputSlot : algorithmInstance.getOutputSlots()) {
            ACAQDataSlot outputSlot = getOutputSlot("Data " + wrappedOutputSlot.getName());
            for (int row = 0; row < wrappedOutputSlot.getRowCount(); ++row) {
                ACAQData data = wrappedOutputSlot.getData(row, ACAQData.class);
                List<ACAQAnnotation> traits = wrappedOutputSlot.getAnnotations(row);
                for (Map.Entry<String, String> entry : newParameters.entrySet()) {
                    traits.add(new ACAQAnnotation(entry.getKey(), "" + parameters.get(entry.getKey()).get(Object.class)));
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
            if (!ReflectionUtils.isAssignableTo(entry.getValue().getClass(), parameterAccess.getFieldClass())) {
                throw new UnsupportedOperationException("Cannot assign parameter '" + entry.getKey() +
                        "' and value '" + entry.getValue() + "' to " + algorithmInstance);
            }
            parameterAccess.set(entry.getValue());
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (algorithmInstance == null) {
            report.reportIsInvalid("No algorithm selected!",
                    "You have to select which algorithm should be executed.",
                    "Please select an algorithm in the parameter panel.",
                    this);
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

    @ACAQParameter("algorithm-parameters")
    public ACAQGraphNode getAlgorithmInstance() {
        return algorithmInstance;
    }

    private void updateAlgorithmInstance() {
        algorithmInstance.getEventBus().register(this);
        getEventBus().post(new ParameterStructureChangedEvent(this));

        ((MultiParameterAlgorithmSlotConfiguration) getSlotConfiguration()).setAlgorithmInstance(algorithmInstance);
    }
}
