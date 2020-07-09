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

package org.hkijena.pipelinej.extensions.multiparameters.algorithms;

import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.data.ACAQSlotDefinition;
import org.hkijena.pipelinej.api.data.ACAQSlotType;
import org.hkijena.pipelinej.extensions.multiparameters.datatypes.ParametersData;

import java.util.HashMap;
import java.util.Map;

/**
 * Slot configuration for {@link MultiParameterAlgorithm}
 */
public class MultiParameterAlgorithmSlotConfiguration extends ACAQDefaultMutableSlotConfiguration {

    private ACAQGraphNode algorithmInstance;
    private boolean isLoading = false;

    /**
     * Creates a new instance
     */
    public MultiParameterAlgorithmSlotConfiguration() {
    }

    @Override
    public ACAQSlotDefinition addSlot(String name, ACAQSlotDefinition definition, boolean user) {
        if (definition.getDataClass() != ParametersData.class) {
            if (!isLoading) {
                // Add slots to the wrapped algorithm
                ACAQDefaultMutableSlotConfiguration instanceSlotConfiguration = (ACAQDefaultMutableSlotConfiguration) algorithmInstance.getSlotConfiguration();
                instanceSlotConfiguration.addSlot(name, definition, false);
            }
            name = "Data " + name;
        }
        return super.addSlot(name, definition, user);
    }

    private void updateConfiguration() {
        isLoading = true;
        // Remove all slots
        setInputSealed(false);
        setOutputSealed(false);
        setAllowedInputSlotTypes(getUnhiddenRegisteredDataTypes());
        setAllowedOutputSlotTypes(getUnhiddenRegisteredDataTypes());
        clearInputSlots(true);
        clearOutputSlots(true);

        if (algorithmInstance == null) {
            setInputSealed(true);
            setOutputSealed(true);
            return;
        }

        // Add the parameter input slot
        addSlot("Parameters", new ACAQSlotDefinition(ParametersData.class, ACAQSlotType.Input), false);

        // Load configuration from wrapped algorithm
        ACAQDefaultMutableSlotConfiguration instanceSlotConfiguration = (ACAQDefaultMutableSlotConfiguration) algorithmInstance.getSlotConfiguration();
        setAllowInheritedOutputSlots(instanceSlotConfiguration.isAllowInheritedOutputSlots());
        setAllowedInputSlotTypes(instanceSlotConfiguration.getAllowedInputSlotTypes());
        setAllowedOutputSlotTypes(instanceSlotConfiguration.getAllowedOutputSlotTypes());
        setMaxInputSlots(instanceSlotConfiguration.getMaxInputSlots() + 1); // For the parameter slot
        setMaxOutputSlots(instanceSlotConfiguration.getMaxOutputSlots());
        for (Map.Entry<String, ACAQSlotDefinition> entry : instanceSlotConfiguration.getInputSlots().entrySet()) {
            addDataSlot(entry);
        }
        for (Map.Entry<String, ACAQSlotDefinition> entry : instanceSlotConfiguration.getOutputSlots().entrySet()) {
            addDataSlot(entry);
        }
        setInputSealed(instanceSlotConfiguration.isInputSlotsSealed());
        setOutputSealed(instanceSlotConfiguration.isOutputSlotsSealed());

        isLoading = false;
    }

    private void addDataSlot(Map.Entry<String, ACAQSlotDefinition> entry) {
        ACAQSlotDefinition original = entry.getValue();
        String inheritedSlot = original.getInheritedSlot();
        if (inheritedSlot != null && !inheritedSlot.isEmpty() && !"*".equals(inheritedSlot)) {
            inheritedSlot = "Data " + inheritedSlot;
        }
        ACAQSlotDefinition copy = new ACAQSlotDefinition(original.getDataClass(),
                original.getSlotType(),
                original.getName(),
                inheritedSlot);
        copy.setInheritanceConversions(new HashMap<>(original.getInheritanceConversions()));
        addSlot(entry.getKey(), copy, false);
    }

    public ACAQGraphNode getAlgorithmInstance() {
        return algorithmInstance;
    }

    public void setAlgorithmInstance(ACAQGraphNode algorithmInstance) {
        this.algorithmInstance = algorithmInstance;
        updateConfiguration();
    }
}
