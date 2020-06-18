package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.data.ACAQSlotType;

/**
 * Slot configuration that always ensures 1:1 relation between input and output slots
 */
public class ACAQIOSlotConfiguration extends ACAQDefaultMutableSlotConfiguration {
    /**
     * Creates a new instance
     */
    public ACAQIOSlotConfiguration() {
    }

    @Override
    public ACAQSlotDefinition addSlot(String name, ACAQSlotDefinition definition, boolean user) {
        ACAQSlotDefinition newSlot = super.addSlot(name, definition, user);
        if (newSlot.isInput()) {
            ACAQSlotDefinition sisterSlot = new ACAQSlotDefinition(definition.getDataClass(), ACAQSlotType.Output, null);
            super.addSlot(name, sisterSlot, user);
        } else if (newSlot.isOutput()) {
            ACAQSlotDefinition sisterSlot = new ACAQSlotDefinition(definition.getDataClass(), ACAQSlotType.Input, null);
            super.addSlot(name, sisterSlot, user);
        }
        return newSlot;
    }

    @Override
    public void removeInputSlot(String name, boolean user) {
        super.removeInputSlot(name, user);
        super.removeOutputSlot(name, user);
    }

    @Override
    public void removeOutputSlot(String name, boolean user) {
        super.removeOutputSlot(name, user);
        super.removeInputSlot(name, user);
    }
}
