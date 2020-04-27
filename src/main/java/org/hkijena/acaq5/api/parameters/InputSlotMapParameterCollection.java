package org.hkijena.acaq5.api.parameters;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.HashSet;
import java.util.Set;

/**
 * Parameter that holds a value for each data slot
 */
public class InputSlotMapParameterCollection extends SlotMapParameterCollection {
    /**
     * Creates a new instance
     *
     * @param dataClass the data type of the parameter assigned to each slot
     * @param algorithm the algorithm that contains the slots
     */
    public InputSlotMapParameterCollection(Class<?> dataClass, ACAQAlgorithm algorithm) {
        super(dataClass, algorithm);
    }

    @Override
    public void updateSlots() {
        if (getAlgorithm() != null) {
            Set<String> toRemove = new HashSet<>();
            for (String slotName : getParameters().keySet()) {
                if (!getAlgorithm().hasInputSlot(slotName)) {
                    toRemove.add(slotName);
                }
            }
            for (String slotName : toRemove) {
                removeParameter(slotName);
            }
            for (ACAQDataSlot slot : getAlgorithm().getInputSlots()) {
                if (!containsKey(slot.getName())) {
                    addParameter(slot.getName(), getDataClass());
                }
            }
        }
    }
}
