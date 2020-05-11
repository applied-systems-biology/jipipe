package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.parameters.ACAQMutableParameterAccess;
import org.hkijena.acaq5.utils.ReflectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Parameter that holds a value for each data slot
 */
public class OutputSlotMapParameterCollection extends SlotMapParameterCollection {
    /**
     * Creates a new instance
     *
     * @param dataClass            the data type of the parameter assigned to each slot
     * @param algorithm            the algorithm that contains the slots
     * @param newInstanceGenerator optional method that generated new instances. Can be null
     * @param initialize           If true, update the slots on creation
     */
    public OutputSlotMapParameterCollection(Class<?> dataClass, ACAQAlgorithm algorithm, Supplier<Object> newInstanceGenerator, boolean initialize) {
        super(dataClass, algorithm, newInstanceGenerator, initialize);
    }

    @Override
    public void updateSlots() {
        if (getAlgorithm() != null) {
            Set<String> toRemove = new HashSet<>();
            for (String slotName : getParameters().keySet()) {
                if (!getAlgorithm().hasOutputSlot(slotName)) {
                    toRemove.add(slotName);
                }
            }
            for (String slotName : toRemove) {
                removeParameter(slotName);
            }
            for (ACAQDataSlot slot : getAlgorithm().getOutputSlots()) {
                if (!containsKey(slot.getName())) {
                    ACAQMutableParameterAccess access = addParameter(slot.getName(), getDataClass());
                    if (getNewInstanceGenerator() != null)
                        access.set(getNewInstanceGenerator().get());
                    else
                        access.set(ReflectionUtils.newInstance(getDataClass()));
                }
            }
        }
    }
}
