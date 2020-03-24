package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;

public class ACAQIOSlotConfiguration extends ACAQMutableSlotConfiguration {
    public ACAQIOSlotConfiguration() {
    }

    @Override
    public void addSlot(String name, ACAQSlotDefinition definition) {
        if (definition.getSlotType() == ACAQDataSlot.SlotType.Output) {
            if (!name.startsWith("Output "))
                name = "Output " + name;
            String inputName = name.substring("Output ".length());
            if (!getSlots().containsKey(inputName)) {
                addSlot(inputName, new ACAQSlotDefinition(definition.getDataClass(), ACAQDataSlot.SlotType.Input, inputName, null));
                return;
            }
        }
        super.addSlot(name, definition);
        if (definition.getSlotType() == ACAQDataSlot.SlotType.Input) {
            String outputName = "Output " + name;
            if (!getSlots().containsKey(outputName)) {
                addSlot(outputName, new ACAQSlotDefinition(definition.getDataClass(), ACAQDataSlot.SlotType.Output, outputName, null));
            }
        }
    }

    @Override
    public void removeSlot(String name) {
        ACAQSlotDefinition slot = getSlots().get(name);
        super.removeSlot(name);

        if (slot.getSlotType() == ACAQDataSlot.SlotType.Input) {
            String outputName = "Output " + name;
            if (hasSlot(outputName)) {
                super.removeSlot(outputName);
            }
        } else if (slot.getSlotType() == ACAQDataSlot.SlotType.Output) {
            String inputName = name.substring("Output ".length());
            if (hasSlot(inputName)) {
                super.removeSlot(inputName);
            }
        }
    }
}
