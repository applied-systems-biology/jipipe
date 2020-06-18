package org.hkijena.acaq5.api.data;

import java.util.Set;

/**
 * A slot configuration that can be changed.
 */
public interface ACAQMutableSlotConfiguration extends ACAQSlotConfiguration {
    Set<Class<? extends ACAQData>> getAllowedInputSlotTypes();

    Set<Class<? extends ACAQData>> getAllowedOutputSlotTypes();

    boolean isAllowInheritedOutputSlots();

    boolean allowsInputSlots();

    boolean allowsOutputSlots();

    boolean isInputSlotsSealed();

    boolean isOutputSlotsSealed();

    void removeInputSlot(String name, boolean user);

    void removeOutputSlot(String name, boolean user);

    boolean canAddInputSlot();

    boolean canModifyInputSlots();

    boolean canModifyOutputSlots();

    boolean canAddOutputSlot();

    boolean canCreateCompatibleInputSlot(Class<? extends ACAQData> acceptedDataType);

    ACAQSlotDefinition addSlot(String name, ACAQSlotDefinition definition, boolean user);

    void moveDown(String name, ACAQSlotType slotType);

    void moveUp(String name, ACAQSlotType slotType);
}
