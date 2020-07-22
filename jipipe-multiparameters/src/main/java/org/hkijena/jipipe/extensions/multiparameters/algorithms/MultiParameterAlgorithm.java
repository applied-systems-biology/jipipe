/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.multiparameters.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeNodeInfoRef;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

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
public class MultiParameterAlgorithm extends JIPipeAlgorithm {

    private JIPipeGraphNode algorithmInstance;

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public MultiParameterAlgorithm(JIPipeNodeInfo info) {
        super(info, new MultiParameterAlgorithmSlotConfiguration());
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
            this.algorithmInstance = other.algorithmInstance.getInfo().clone(other.algorithmInstance);
            this.algorithmInstance.getEventBus().register(this);
        }
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        checkInputSlots();

        // Backup default parameters
        Map<String, Object> defaultParameterSnapshot = getDefaultParameterSnapshot();
        JIPipeDataSlot parameterSlot = getInputSlot("Parameters");
        Map<String, JIPipeParameterAccess> parameters = JIPipeParameterTree.getParameters(algorithmInstance);
        Map<String, String> changedParameterTraits = new HashMap<>();

        for (int row = 0; row < parameterSlot.getRowCount(); ++row) {
            if (isCancelled.get())
                return;
            ParametersData parametersData = parameterSlot.getData(row, ParametersData.class);
            for (Map.Entry<String, Object> entry : parametersData.getParameterData().entrySet()) {
                JIPipeParameterAccess parameterAccess = parameters.getOrDefault(entry.getKey(), null);
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
            JIPipeRunnerSubStatus parameterProgress = subProgress.resolve("Parameter set " + (row + 1) + " / " + parameterSlot.getRowCount());
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
        JIPipeParameterTree collection = new JIPipeParameterTree(algorithmInstance);
        for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(Object.class));
        }
        return result;
    }

    private void passOutputData(Map<String, JIPipeParameterAccess> parameters, Map<String, String> newParameters) {
        for (JIPipeDataSlot wrappedOutputSlot : algorithmInstance.getOutputSlots()) {
            JIPipeDataSlot outputSlot = getOutputSlot("Data " + wrappedOutputSlot.getName());
            for (int row = 0; row < wrappedOutputSlot.getRowCount(); ++row) {
                JIPipeData data = wrappedOutputSlot.getData(row, JIPipeData.class);
                List<JIPipeAnnotation> traits = wrappedOutputSlot.getAnnotations(row);
                for (Map.Entry<String, String> entry : newParameters.entrySet()) {
                    traits.add(new JIPipeAnnotation(entry.getKey(), "" + parameters.get(entry.getKey()).get(Object.class)));
                }
                outputSlot.addData(data, traits);
            }
            wrappedOutputSlot.clearData(false);
        }
    }

    private void passInputData() {
        for (JIPipeDataSlot wrappedInputSlot : algorithmInstance.getInputSlots()) {
            JIPipeDataSlot inputSlot = getInputSlot("Data " + wrappedInputSlot.getName());
            wrappedInputSlot.clearData(false);
            wrappedInputSlot.copyFrom(inputSlot);
        }
    }

    private void passParameters(ParametersData parametersData, Map<String, JIPipeParameterAccess> parameters) {
        for (Map.Entry<String, Object> entry : parametersData.getParameterData().entrySet()) {
            JIPipeParameterAccess parameterAccess = parameters.getOrDefault(entry.getKey(), null);
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
    public void reportValidity(JIPipeValidityReport report) {
        if (algorithmInstance == null) {
            report.reportIsInvalid("No algorithm selected!",
                    "You have to select which algorithm should be executed.",
                    "Please select an algorithm in the parameter panel.",
                    this);
        }
    }

    public List<JIPipeDataSlot> getDataInputSlots() {
        return getInputSlots().stream().filter(s -> s.getName().startsWith("Data ")).collect(Collectors.toList());
    }

    private void checkInputSlots() {
        List<JIPipeDataSlot> dataInputSlots = getDataInputSlots();
        if (dataInputSlots.isEmpty())
            return;
        int rows = dataInputSlots.get(0).getRowCount();
        for (int i = 1; i < dataInputSlots.size(); ++i) {
            if (rows != dataInputSlots.get(i).getRowCount())
                throw new RuntimeException("Data input slots have a different row count!");
        }
    }

    @JIPipeParameter("algorithm-type")
    @JIPipeDocumentation(name = "Algorithm", description = "The algorithm the parameters are created for")
    public JIPipeNodeInfoRef getNodeInfo() {
        if (algorithmInstance == null) {
            return new JIPipeNodeInfoRef();
        } else {
            return new JIPipeNodeInfoRef(algorithmInstance.getInfo());
        }
    }

    @JIPipeParameter("algorithm-type")
    public void setNodeInfo(JIPipeNodeInfoRef nodeInfo) {
        if (nodeInfo.getInfo() != null) {
            if (algorithmInstance != null) {
                algorithmInstance.getEventBus().unregister(this);
            }
            algorithmInstance = nodeInfo.getInfo().newInstance();
        } else {
            algorithmInstance = null;
        }
        updateAlgorithmInstance();
    }

    @JIPipeParameter("algorithm-parameters")
    public JIPipeGraphNode getAlgorithmInstance() {
        return algorithmInstance;
    }

    private void updateAlgorithmInstance() {
        algorithmInstance.getEventBus().register(this);
        getEventBus().post(new ParameterStructureChangedEvent(this));

        ((MultiParameterAlgorithmSlotConfiguration) getSlotConfiguration()).setAlgorithmInstance(algorithmInstance);
    }
}
