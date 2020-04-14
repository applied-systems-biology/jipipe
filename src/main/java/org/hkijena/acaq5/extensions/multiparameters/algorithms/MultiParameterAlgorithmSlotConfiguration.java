package org.hkijena.acaq5.extensions.multiparameters.algorithms;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;

import java.util.Map;

/**
 * Slot configuration for {@link MultiParameterAlgorithm}
 */
public class MultiParameterAlgorithmSlotConfiguration extends ACAQMutableSlotConfiguration {

    private ACAQAlgorithm algorithmInstance;
    private boolean isLoading = false;

    /**
     * Creates a new instance
     */
    public MultiParameterAlgorithmSlotConfiguration() {
    }

    @Override
    public void addSlot(String name, ACAQSlotDefinition definition, boolean user) {
        if (definition.getDataClass() != ParametersData.class) {
            if (!isLoading) {
                // Add slots to the wrapped algorithm
                ACAQMutableSlotConfiguration instanceSlotConfiguration = (ACAQMutableSlotConfiguration) algorithmInstance.getSlotConfiguration();
                instanceSlotConfiguration.addSlot(name, definition, false);
            }
            name = "Data " + name;
        }
        super.addSlot(name, definition, user);
    }

    private void updateConfiguration() {
        isLoading = true;
        // Remove all slots
        setInputSealed(false);
        setOutputSealed(false);
        setAllowedInputSlotTypes(getUnhiddenRegisteredDataTypes());
        setAllowedOutputSlotTypes(getUnhiddenRegisteredDataTypes());
        clearInputSlots();
        clearOutputSlots();

        if (algorithmInstance == null) {
            setInputSealed(true);
            setOutputSealed(true);
            return;
        }

        // Add the parameter input slot
        addSlot("Parameters", new ACAQSlotDefinition(ParametersData.class, ACAQDataSlot.SlotType.Input), false);

        // Load configuration from wrapped algorithm
        ACAQMutableSlotConfiguration instanceSlotConfiguration = (ACAQMutableSlotConfiguration) algorithmInstance.getSlotConfiguration();
        setAllowedInputSlotTypes(instanceSlotConfiguration.getAllowedInputSlotTypes());
        setAllowedOutputSlotTypes(instanceSlotConfiguration.getAllowedOutputSlotTypes());
        setMaxInputSlots(instanceSlotConfiguration.getMaxInputSlots() + 1); // For the parameter slot
        setMaxOutputSlots(instanceSlotConfiguration.getMaxOutputSlots());
        for (Map.Entry<String, ACAQSlotDefinition> entry : instanceSlotConfiguration.getSlots().entrySet()) {
            addSlot(entry.getKey(), entry.getValue(), false);
        }
        setInputSealed(instanceSlotConfiguration.isInputSlotsSealed());
        setOutputSealed(instanceSlotConfiguration.isOutputSlotsSealed());

        isLoading = false;
    }

    public ACAQAlgorithm getAlgorithmInstance() {
        return algorithmInstance;
    }

    public void setAlgorithmInstance(ACAQAlgorithm algorithmInstance) {
        this.algorithmInstance = algorithmInstance;
        updateConfiguration();
    }
}
